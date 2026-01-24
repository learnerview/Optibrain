package com.optibrain.recommendation.service;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.service.AuditService;
import com.optibrain.cloud.adapter.CloudAdapter;
import com.optibrain.cloud.config.CloudConfig;
import com.optibrain.cloud.remediator.CloudRemediator;
import com.optibrain.metrics.model.MetricData;
import com.optibrain.pricing.model.InstancePricing;
import com.optibrain.pricing.service.PricingService;
import com.optibrain.recommendation.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

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
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-mode", havingValue = "false", matchIfMissing = true)
public class RecommendationService implements OptimizationService {

    private final PricingService pricingService;
    private final CloudAdapter cloudAdapter;
    private final CloudRemediator cloudRemediator;
    private final CloudConfig cloudConfig;
    private final AuditService auditService;
    private final Map<String, Recommendation> store = new HashMap<>();

    /**
     * Generates comprehensive rightsizing recommendations based on current metrics
     * Requires CLOUD_INTELLIGENCE_ANALYST role or higher
     */
    @PreAuthorize("hasAnyRole('CLOUD_INTELLIGENCE_ANALYST', 'ADMIN', 'OWNER')")
    public List<Recommendation> generateRightsizing(MetricData metrics) {
        List<Recommendation> recs = new ArrayList<>();
        // Simulate inventory: 3 instances with types
        List<String> instanceIds = List.of("i-01", "i-02", "i-03");
        List<String> currentTypes = List.of("t3.large", "t3.medium", "m5.large");

        for (int i = 0; i < instanceIds.size(); i++) {
            String id = instanceIds.get(i);
            String currentType = currentTypes.get(i);
            double cpu = metrics.getCpuUtilization() + (Math.random() * 20 - 10);
            double mem = metrics.getMemoryUtilization() + (Math.random() * 15 - 7.5);

            Recommendation rec = evaluateInstance(id, currentType, cpu, mem);
            if (rec != null) {
                store.put(rec.getId(), rec);
                recs.add(rec);
            }
        }

        // Expand beyond rightsizing: orphan cleanup + RI/SP optimization
        for (Recommendation r : generateOrphanCleanup(metrics)) {
            store.put(r.getId(), r);
            recs.add(r);
        }
        for (Recommendation r : generateCommitmentOptimization(metrics)) {
            store.put(r.getId(), r);
            recs.add(r);
        }
        return recs;
    }

    private List<Recommendation> generateOrphanCleanup(MetricData metrics) {
        // Simulation-first: in MOCK mode, create a couple of orphan candidates.
        // In AWS mode, this should be extended to query unattached EBS, unassociated EIPs, idle ALBs, etc.
        List<Recommendation> recs = new ArrayList<>();

        List<String> orphanResourceIds = List.of("eip-orphan-01", "vol-orphan-01");
        for (String rid : orphanResourceIds) {
            double currentHourly = Math.max(0.5, cloudAdapter.getCostEstimate(rid) / 730.0);
            double monthlySavings = currentHourly * 730;
            Recommendation rec = Recommendation.builder()
                    .id(UUID.randomUUID().toString().substring(0, 8))
                    .resourceId(rid)
                    .currentType(cloudAdapter.getResourceType(rid))
                    .recommendedType("TERMINATE")
                    .action("ORPHAN_CLEANUP")
                    .currentHourlyCost(currentHourly)
                    .recommendedHourlyCost(0)
                    .monthlySavings(monthlySavings)
                    .upfrontCost(0)
                    .roiMonthly(null)
                    .paybackDays(0)
                    .confidence(0.75)
                    .utilizationCpu(metrics.getCpuUtilization())
                    .utilizationMemory(metrics.getMemoryUtilization())
                    .reason("Resource appears orphaned/unattached and incurs ongoing cost")
                    .details(Map.of(
                            "resourceType", cloudAdapter.getResourceType(rid),
                            "signal", "simulated_orphan",
                            "recommendedAction", "terminate"
                    ))
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            recs.add(rec);
        }

        // Also flag chronically underutilized instances for decommissioning (simulation-first)
        List<String> instanceCandidates = cloudAdapter.discoverInstances();
        for (String instanceId : instanceCandidates) {
            double cpu = metrics.getCpuUtilization() + (Math.random() * 10 - 5);
            double mem = metrics.getMemoryUtilization() + (Math.random() * 10 - 5);
            if (cpu < 10 && mem < 15) {
                double currentHourly = Math.max(0.01, cloudAdapter.getCostEstimate(instanceId) / 730.0);
                double monthlySavings = currentHourly * 730;
                Recommendation rec = Recommendation.builder()
                        .id(UUID.randomUUID().toString().substring(0, 8))
                        .resourceId(instanceId)
                        .currentType(cloudAdapter.getResourceType(instanceId))
                        .recommendedType("TERMINATE")
                        .action("DECOMMISSION")
                        .currentHourlyCost(currentHourly)
                        .recommendedHourlyCost(0)
                        .monthlySavings(monthlySavings)
                        .upfrontCost(0)
                        .roiMonthly(null)
                        .paybackDays(0)
                        .confidence(0.72)
                        .utilizationCpu(cpu)
                        .utilizationMemory(mem)
                        .reason("Very low utilization indicates resource may be unused and safe to decommission")
                        .details(Map.of(
                                "signal", "low_cpu_and_memory",
                                "cpu", String.format("%.1f%%", cpu),
                                "memory", String.format("%.1f%%", mem)
                        ))
                        .status("PENDING")
                        .createdAt(LocalDateTime.now())
                        .build();
                recs.add(rec);
            }
        }
        return recs;
    }

    private List<Recommendation> generateCommitmentOptimization(MetricData metrics) {
        // Simulation-first RI/SP optimization based on stable baseline usage.
        // Uses current hourly cost as the on-demand baseline and proposes a commitment discount.
        double onDemandHourly = Math.max(0.01, metrics.getHourlyCost());
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
    public List<Recommendation> listPending() {
        return store.values().stream()
                .filter(r -> "PENDING".equals(r.getStatus()))
                .toList();
    }

    /**
     * Approves a recommendation for execution
     * Requires CLOUD_INTELLIGENCE_ANALYST role or higher
     */
    @PreAuthorize("hasAnyRole('CLOUD_INTELLIGENCE_ANALYST', 'ADMIN', 'OWNER')")
    public boolean approve(String id) {
        Recommendation r = store.get(id);
        if (r != null && "PENDING".equals(r.getStatus())) {
            r.setStatus("APPROVED");
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
    public boolean reject(String id) {
        Recommendation r = store.get(id);
        if (r != null && "PENDING".equals(r.getStatus())) {
            r.setStatus("REJECTED");
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
    public boolean execute(String id) {
        Recommendation r = store.get(id);
        if (r != null && "APPROVED".equals(r.getStatus())) {
            if (!shouldExecute(r)) {
                log.info("[RECOMMENDATION] Blocked by safety guardrail: {} ({})", id, r.getAction());
                recordAudit(r, "BLOCKED", "SKIPPED", "Safety guardrail triggered");
                return false;
            }

            boolean success = executeRecommendation(r);
            String mode = cloudConfig.isDryRun() ? "DRY_RUN" : "EXECUTED";
            String status = success ? "SUCCESS" : "FAILED";

            r.setStatus("EXECUTED");
            r.setExecutedAt(LocalDateTime.now());
            recordAudit(r, mode, status, null);
            return success;
        }
        return false;
    }

    private boolean executeRecommendation(Recommendation r) {
        String action = r.getAction();
        if ("RIGHTSIZE".equalsIgnoreCase(action)) {
            return cloudRemediator.applyRightsizing(r.getResourceId(), r.getRecommendedType());
        }
        if ("DECOMMISSION".equalsIgnoreCase(action) || "ORPHAN_CLEANUP".equalsIgnoreCase(action)) {
            if (cloudConfig.isDryRun()) {
                log.info("[DRY-RUN] Would terminate resource {}", r.getResourceId());
                return true;
            }
            return cloudAdapter.terminateResource(r.getResourceId());
        }
        if ("RI_OPTIMIZATION".equalsIgnoreCase(action) || "SP_OPTIMIZATION".equalsIgnoreCase(action)) {
            // Simulation-first: record as success; real implementation would call AWS SavingsPlans/RI APIs.
            if (cloudConfig.isDryRun()) {
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
        if (cloudConfig.isDryRun()) return true;

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
        // Simple bridge for the contract
        return listPending().stream()
            .map(r -> Map.<String, Object>of(
                "resource", r.getResourceId(),
                "issue", r.getReason(),
                "recommendation", r.getAction(),
                "monthlySavings", r.getMonthlySavings()
            ))
            .toList();
    }

    /**
     * Returns recommendation history for audit purposes
     * Read access for all authenticated users
     */
    @PreAuthorize("isAuthenticated()")
    public List<Recommendation> history() {
        return new ArrayList<>(store.values());
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
                .providerSource(cloudAdapter.getProviderName())
                .build();
        auditService.record(logEntry);
    }
}
