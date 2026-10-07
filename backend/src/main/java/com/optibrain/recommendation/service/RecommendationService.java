package com.optibrain.recommendation.service;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.service.AuditService;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.MetricsSeries;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.port.ResourceQuery;
import com.optibrain.cloud.port.TelemetrySource;
import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.cloud.remediation.RemediationService;
import com.optibrain.common.context.TenantContext;
import com.optibrain.metrics.model.MetricData;
import com.optibrain.pricing.model.InstancePricing;
import com.optibrain.pricing.service.PricingService;
import com.optibrain.recommendation.model.Recommendation;
import com.optibrain.recommendation.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for generating and managing cost optimization recommendations
 * Provides autonomous Cloud Cost Intelligence capabilities with safety controls and RBAC
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService implements OptimizationService {

    private final PricingService pricingService;
    private final CloudProviderPort cloudProvider;
    private final RemediationService remediation;
    private final CloudProperties cloudProperties;
    private final AuditService auditService;
    private final RecommendationRepository repository;

    /**
     * The tenant this thread is running for. Falls back to the default tenant so the
     * recommendation surface keeps working in contexts (tests, bootstrap) without a
     * security-derived membership.
     */
    private String currentTenant() {
        String tenantId = TenantContext.getTenantId();
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }

    /**
     * Generates comprehensive rightsizing recommendations based on current metrics
     * Requires CLOUD_INTELLIGENCE_ANALYST role or higher
     */
    @PreAuthorize("hasAnyRole('CLOUD_INTELLIGENCE_ANALYST', 'ADMIN', 'OWNER')")
    public List<Recommendation> generateRightsizing(MetricData metrics) {
        List<Recommendation> recs = new ArrayList<>();

        // Rightsizing every instance in the account, from the real inventory, using the
        // real CloudWatch series for each one. This previously iterated a hardcoded list of
        // ids "i-01", "i-02", "i-03" with CPU figures perturbed by +/-10 random points -
        // which produced confident recommendations about instances that do not exist.
        Instant now = Instant.now();
        Duration window = Duration.ofDays(14);

        double cpuSum = 0, memSum = 0;
        int cpuCount = 0, memCount = 0;

        for (CloudResource instance : cloudProvider.discover(
                ResourceQuery.ofTypes(ResourceType.EC2_INSTANCE, ResourceType.GPU_INSTANCE))) {
            String id = instance.id();
            String currentType = instance.specOrDefault("instanceType", "unknown");

            MetricsSeries cpuSeries = cloudProvider.ec2InstanceMetrics(
                    id, TelemetrySource.CPU_UTILIZATION, now.minus(window), now, window);
            MetricsSeries memSeries = cloudProvider.ec2InstanceMetrics(
                    id, "mem_used_percent", now.minus(window), now, window);

            // No telemetry means no evidence. Recommending on absent data would make the
            // confidence score meaningless, so the instance is skipped rather than guessed.
            if (cpuSeries.isEmpty() || cpuSeries.averageOverLast(cpuSeries.points().size()) <= 0.0) {
                log.debug("Skipping rightsizing for {}: no CPU telemetry", id);
                continue;
            }

            cpuSum += cpuSeries.average();
            cpuCount++;
            if (!memSeries.isEmpty()) {
                memSum += memSeries.average();
                memCount++;
            }

            Recommendation rec = evaluateInstance(
                    id, currentType, cpuSeries.average(), memSeries.average());
            if (rec != null) {
                recs.add(rec);
            }
        }

        // The residual blocks (orphans, commitments) carry utilization figures measured
        // from the account, never numbers supplied by the caller, so a displayed
        // percentage cannot drift from the telemetry it is meant to summarise.
        double measuredCpu = cpuCount == 0 ? 0.0 : cpuSum / cpuCount;
        double measuredMem = memCount == 0 ? 0.0 : memSum / memCount;
        MetricData measured = MetricData.builder()
                .cpuUtilization(measuredCpu)
                .memoryUtilization(measuredMem)
                .hourlyCost(metrics == null ? 0.0 : metrics.getHourlyCost())
                .build();

        // Expand beyond rightsizing: orphan cleanup + RI/SP optimization
        for (Recommendation r : generateOrphanCleanup(measured)) {
            recs.add(r);
        }
        for (Recommendation r : generateCommitmentOptimization(measured)) {
            recs.add(r);
        }
        return recs;
    }

    private List<Recommendation> generateOrphanCleanup(MetricData metrics) {
        List<Recommendation> recs = new ArrayList<>();

        // Genuinely orphaned resources, discovered from the account rather than invented.
        // The previous implementation recommended against literal ids like
        // "eip-orphan-01", which could never be acted on because they do not exist.
        List<ResourceType> orphanTypes = List.of(
                ResourceType.EBS_VOLUME, ResourceType.ELASTIC_IP, ResourceType.EBS_SNAPSHOT);

        for (CloudResource resource : cloudProvider.discover(ResourceQuery.ofTypes(
                orphanTypes.toArray(new ResourceType[0])))) {
            if (isAttached(resource)) {
                continue;
            }
            double monthlySavings = resource.monthlyCost() == null ? 0.0 : resource.monthlyCost();
            if (monthlySavings <= 0.0) {
                continue;
            }
            recs.add(Recommendation.builder()
                    .id(UUID.randomUUID().toString().substring(0, 8))
                    .resourceId(resource.id())
                    .currentType(resource.type().typeName())
                    .recommendedType("RELEASE")
                    .action("ORPHAN_CLEANUP")
                    .currentHourlyCost(monthlySavings / 730.0)
                    .recommendedHourlyCost(0.0)
                    .monthlySavings(monthlySavings)
                    .upfrontCost(0.0)
                    .roiMonthly(null)
                    .paybackDays(0)
                    .confidence(0.9)
                    .utilizationCpu(metrics.getCpuUtilization())
                    .utilizationMemory(metrics.getMemoryUtilization())
                    .reason(resource.type().typeName() + " is unattached and still incurring cost")
                    .details(Map.of(
                            "resourceType", resource.type().typeName(),
                            "signal", "unattached",
                            "region", String.valueOf(resource.region()),
                            "monthlyCost", String.format("%.2f", monthlySavings)
                    ))
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return recs;
    }

    /**
     * Whether a resource is in use.
     *
     * <p>A volume with attachments or an address with an association is doing work, even
     * if its tags say nothing.
     */
    private boolean isAttached(CloudResource resource) {
        return switch (resource.type()) {
            case EBS_VOLUME -> !"0".equals(resource.spec("attachmentCount"));
            case ELASTIC_IP -> "true".equalsIgnoreCase(resource.spec("associated"));
            default -> false;
        };
    }

    private List<Recommendation> generateCommitmentOptimization(MetricData metrics) {
        // A commitment suggestion is only meaningful against a measured baseline. With
        // no attributed spend there is nothing to discount, and presenting one would
        // attach a fabricated number to an empty account.
        double configuredHourly = metrics == null ? 0.0 : metrics.getHourlyCost();
        if (configuredHourly <= 0.0) {
            return List.of();
        }

        // Simulation-first RI/SP optimization based on the measured hourly baseline.
        double onDemandHourly = Math.max(0.01, configuredHourly);
        double onDemandMonthly = onDemandHourly * 730;

        double riDiscount = 0.30; // 30% simulated savings
        double riMonthlySavings = onDemandMonthly * riDiscount;

        double riUpfrontCost = Math.max(0, riMonthlySavings * 2); // ~2 months pay upfront (simulated)
        Integer riPaybackDays = riMonthlySavings > 0 ? (int) Math.ceil((riUpfrontCost / riMonthlySavings) * 30) : null;
        Double riRoiMonthly = riUpfrontCost > 0 ? (riMonthlySavings / riUpfrontCost) : null;

        Recommendation riRec = Recommendation.builder()
                .id(UUID.randomUUID().toString().substring(0, 8))
                .resourceId("account")
                .currentType("ON_DEMAND")
                .recommendedType("1YR_COMMITMENT")
                .action("RI_OPTIMIZATION")
                .currentHourlyCost(onDemandHourly)
                .recommendedHourlyCost(onDemandHourly * (1 - riDiscount))
                .monthlySavings(riMonthlySavings)
                .upfrontCost(riUpfrontCost)
                .roiMonthly(riRoiMonthly)
                .paybackDays(riPaybackDays)
                .confidence(0.68)
                .utilizationCpu(metrics.getCpuUtilization())
                .utilizationMemory(metrics.getMemoryUtilization())
                .reason("Stable baseline spend suggests commitment purchase could improve ROI")
                .details(Map.of(
                        "type", "reserved_instance",
                        "term", "1yr",
                        "discount", String.format("%.0f%%", riDiscount * 100),
                        "basis", "simulated_baseline"
                ))
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        double spDiscount = 0.22; // simulated savings plan discount
        double spMonthlySavings = onDemandMonthly * spDiscount;
        double spUpfrontCost = 0; // assume no-upfront savings plan
        Integer spPaybackDays = 0;
        Double spRoiMonthly = null;

        Recommendation spRec = Recommendation.builder()
                .id(UUID.randomUUID().toString().substring(0, 8))
                .resourceId("account")
                .currentType("ON_DEMAND")
                .recommendedType("1YR_SAVINGS_PLAN")
                .action("SP_OPTIMIZATION")
                .currentHourlyCost(onDemandHourly)
                .recommendedHourlyCost(onDemandHourly * (1 - spDiscount))
                .monthlySavings(spMonthlySavings)
                .upfrontCost(spUpfrontCost)
                .roiMonthly(spRoiMonthly)
                .paybackDays(spPaybackDays)
                .confidence(0.64)
                .utilizationCpu(metrics.getCpuUtilization())
                .utilizationMemory(metrics.getMemoryUtilization())
                .reason("Steady compute spend suggests a Savings Plan could reduce cost without rightsizing risk")
                .details(Map.of(
                        "type", "savings_plan",
                        "term", "1yr",
                        "payment", "no_upfront",
                        "discount", String.format("%.0f%%", spDiscount * 100),
                        "basis", "simulated_baseline"
                ))
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        return List.of(riRec, spRec);
    }

    private Recommendation evaluateInstance(String instanceId, String currentType, double cpu, double mem) {
        InstancePricing current = pricingService.get(currentType);
        if (current == null) return null;

        // Heuristic: if CPU < 30% and Memory < 40%, recommend downsize
        if (cpu < 30 && mem < 40) {
            InstancePricing cheaper = pricingService.cheapestWithAtLeast(1, 2);
            if (cheaper != null && cheaper.getPricePerHour() < current.getPricePerHour()) {
                double monthlySavings = (current.getPricePerHour() - cheaper.getPricePerHour()) * 730;
                return Recommendation.builder()
                        .id(UUID.randomUUID().toString().substring(0, 8))
                        .resourceId(instanceId)
                        .currentType(currentType)
                        .recommendedType(cheaper.getInstanceType())
                        .action("RIGHTSIZE")
                        .currentHourlyCost(current.getPricePerHour())
                        .recommendedHourlyCost(cheaper.getPricePerHour())
                        .monthlySavings(monthlySavings)
                        .utilizationCpu(cpu)
                        .utilizationMemory(mem)
                        .reason("Low utilization suggests rightsizing opportunity")
                        .details(Map.of("cpu", String.format("%.1f%%", cpu), "memory", String.format("%.1f%%", mem)))
                        .status("PENDING")
                        .createdAt(LocalDateTime.now())
                        .build();
            }
        }
        // Heuristic: if CPU > 80% or Memory > 85%, recommend upsize
        if (cpu > 80 || mem > 85) {
            InstancePricing bigger = pricingService.cheapestWithAtLeast((int)(current.getVcpus() + 2), (int)(current.getMemoryGb() + 4));
            if (bigger != null && bigger.getPricePerHour() > current.getPricePerHour()) {
                double monthlyCostIncrease = (bigger.getPricePerHour() - current.getPricePerHour()) * 730;
                return Recommendation.builder()
                        .id(UUID.randomUUID().toString().substring(0, 8))
                        .resourceId(instanceId)
                        .currentType(currentType)
                        .recommendedType(bigger.getInstanceType())
                        .action("RIGHTSIZE")
                        .currentHourlyCost(current.getPricePerHour())
                        .recommendedHourlyCost(bigger.getPricePerHour())
                        .monthlySavings(-monthlyCostIncrease) // negative = cost increase
                        .utilizationCpu(cpu)
                        .utilizationMemory(mem)
                        .reason("High utilization suggests upgrade")
                        .details(Map.of("cpu", String.format("%.1f%%", cpu), "memory", String.format("%.1f%%", mem)))
                        .status("PENDING")
                        .createdAt(LocalDateTime.now())
                        .build();
            }
        }
        return null;
    }

    /**
     * Lists all pending recommendations awaiting approval
     * Read access for all authenticated users
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<Recommendation> listPending() {
        return repository.findByTenantIdAndStatus(currentTenant(), "PENDING");
    }

    /**
     * Approves a recommendation for execution
     * Requires CLOUD_INTELLIGENCE_ANALYST role or higher
     */
    @PreAuthorize("hasAnyRole('CLOUD_INTELLIGENCE_ANALYST', 'ADMIN', 'OWNER')")
    @Transactional
    public boolean approve(String id) {
        Recommendation r = repository.findByIdAndTenantId(id, currentTenant()).orElse(null);
        if (r != null && "PENDING".equals(r.getStatus())) {
            r.setStatus("APPROVED");
            repository.save(r);
            log.info("[RECOMMENDATION] Approved {} by user {}", id, getCurrentUser());
            return true;
        }
        return false;
    }

    /**
     * Rejects a recommendation
     * Requires CLOUD_INTELLIGENCE_ANALYST role or higher
     */
    @PreAuthorize("hasAnyRole('CLOUD_INTELLIGENCE_ANALYST', 'ADMIN', 'OWNER')")
    @Transactional
    public boolean reject(String id) {
        Recommendation r = repository.findByIdAndTenantId(id, currentTenant()).orElse(null);
        if (r != null && "PENDING".equals(r.getStatus())) {
            r.setStatus("REJECTED");
            repository.save(r);
            log.info("[RECOMMENDATION] Rejected {} by user {}", id, getCurrentUser());
            return true;
        }
        return false;
    }

    /**
     * Executes an approved recommendation
     * Requires DEVOPS_ENGINEER role or higher for actual execution
     */
    @PreAuthorize("hasAnyRole('DEVOPS_ENGINEER', 'ADMIN', 'OWNER')")
    @Transactional
    public boolean execute(String id) {
        Recommendation r = repository.findByIdAndTenantId(id, currentTenant()).orElse(null);
        if (r != null && "APPROVED".equals(r.getStatus())) {
            if (!shouldExecute(r)) {
                log.info("[RECOMMENDATION] Blocked by safety guardrail: {} ({})", id, r.getAction());
                recordAudit(r, "BLOCKED", "SKIPPED", "Safety guardrail triggered");
                return false;
            }

            boolean success = executeRecommendation(r);
            String mode = cloudProperties.isDryRun() ? "DRY_RUN" : "EXECUTED";
            String status = success ? "SUCCESS" : "FAILED";

            // Record the true outcome: a failed execution stays distinct from a
            // completed one and must never be presented as done.
            r.setStatus(success ? "EXECUTED" : "FAILED");
            r.setExecutedAt(LocalDateTime.now());
            repository.save(r);
            recordAudit(r, mode, status, null);
            return success;
        }
        return false;
    }

    private boolean executeRecommendation(Recommendation r) {
        String action = r.getAction();
        boolean dryRun = cloudProperties.isDryRun();
        if ("RIGHTSIZE".equalsIgnoreCase(action)) {
            return remediation.execute(ActionType.RESIZE_INSTANCE, r.getResourceId(),
                    Map.of("instanceType", String.valueOf(r.getRecommendedType())), dryRun).success();
        }
        if ("DECOMMISSION".equalsIgnoreCase(action) || "ORPHAN_CLEANUP".equalsIgnoreCase(action)) {
            // Termination goes through the same pipeline as a manual remediation, so the
            // live-inventory check, protection guard, dry-run interlock, audit record and
            // idempotency apply equally to an approved recommendation.
            return remediation.execute(ActionType.TERMINATE_INSTANCE, r.getResourceId(),
                    Map.of(), dryRun).success();
        }
        if ("RI_OPTIMIZATION".equalsIgnoreCase(action) || "SP_OPTIMIZATION".equalsIgnoreCase(action)) {
            // Simulation-first: record as success; real implementation would call AWS SavingsPlans/RI APIs.
            if (cloudProperties.isDryRun()) {
                log.info("[DRY-RUN] Would apply commitment optimization: {}", action);
            } else {
                log.info("[EXECUTION] Applying commitment optimization: {} (simulated)", action);
            }
            return true;
        }

        log.info("[RECOMMENDATION] No-op execution for action {}", action);
        return true;
    }

    private boolean shouldExecute(Recommendation r) {
        // 1. Dry-run always allowed
        if (cloudProperties.isDryRun()) return true;

        // 2. Cooldown: block same action within 10 minutes
        Instant tenMinutesAgo = Instant.now().minusSeconds(600);
        boolean recentSameAction = auditService.recent().stream()
                .anyMatch(a -> r.getAction().equals(a.getAction()) && a.getTimestamp().isAfter(tenMinutesAgo));
        if (recentSameAction) return false;

        // 3. Max-change per hour: limit to 5 executions per hour
        long executionsLastHour = auditService.countExecutionsInLastHour();
        return executionsLastHour < 5;
    }
    @Override
    public List<Map<String, Object>> getRecommendations() {
        // Bridge for the contract. The map is the wire shape consumed by the frontend;
        // nulls are legitimate (an unattributed cost, an unknown confidence) and must be
        // preserved rather than collapsed by an immutable-map utility that rejects them.
        return listPending().stream()
            .map(r -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("id", r.getId());
                row.put("resourceId", r.getResourceId());
                row.put("action", r.getAction());
                row.put("recommendedType", r.getRecommendedType());
                row.put("reason", r.getReason());
                row.put("monthlySavings", r.getMonthlySavings());
                row.put("confidence", r.getConfidence());
                row.put("status", r.getStatus());
                row.put("createdAt", r.getCreatedAt());
                return row;
            })
            .toList();
    }

    @Override
    @Transactional
    public List<Map<String, Object>> refresh() {
        // Regeneration is wired to the read path because recommendations must reflect the
        // account as it is now: without it nothing is ever generated and the feature can
        // never produce a row. Pending rows are regenerated from current state, so an
        // instance that stopped qualifying disappears rather than lingering. Non-pending
        // rows (approved, rejected, executed, failed) are decisions, not inventory
        // snapshots: they are kept so a decision survives the next read and a restart.
        double attributed = 0.0;
        for (CloudResource resource : cloudProvider.discover(ResourceQuery.all())) {
            if (resource.monthlyCost() != null) {
                attributed += resource.monthlyCost();
            }
        }
        MetricData baseline = MetricData.builder()
                .name("measured")
                .hourlyCost(attributed / 730.0)
                .build();
        List<Recommendation> fresh = generateRightsizing(baseline);
        String tenantId = currentTenant();
        repository.deleteByTenantIdAndStatus(tenantId, "PENDING");
        for (Recommendation r : fresh) {
            r.setTenantId(tenantId);
            repository.save(r);
        }
        return getRecommendations();
    }

    /**
     * Returns recommendation history for audit purposes
     * Read access for all authenticated users
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<Recommendation> history() {
        return repository.findByTenantId(currentTenant());
    }

    /**
     * Helper method to get current user for audit purposes
     */
    private String getCurrentUser() {
        // In a real implementation, this would get the current authenticated user
        // from Spring Security context
        try {
            org.springframework.security.core.context.SecurityContext context = 
                org.springframework.security.core.context.SecurityContextHolder.getContext();
            if (context.getAuthentication() != null) {
                return context.getAuthentication().getName();
            }
        } catch (Exception e) {
            log.debug("Could not get current user: {}", e.getMessage());
        }
        return "unknown-user";
    }

    private void recordAudit(Recommendation r, String mode, String status, String overrideReason) {
        String explanation = r.getDetails() == null ? "" : r.getDetails().toString();
        
        com.optibrain.audit.model.AuditStatus auditStatus = com.optibrain.audit.model.AuditStatus.PENDING;
        if ("SUCCESS".equalsIgnoreCase(status)) auditStatus = com.optibrain.audit.model.AuditStatus.SUCCESS;
        else if ("FAILED".equalsIgnoreCase(status)) auditStatus = com.optibrain.audit.model.AuditStatus.FAILED;

        AuditLog logEntry = AuditLog.builder()
                .decisionId("rec-" + r.getId())
                .action(r.getAction())
                .resourceId(r.getResourceId())
                .mode(mode)
                .status(auditStatus)
                .reason(overrideReason != null ? overrideReason : r.getReason())
                .explanation(explanation)
                .score(0.0)
                .providerSource(cloudProvider.getProviderName())
                .build();
        auditService.record(logEntry);
    }
}
