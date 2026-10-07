package com.optibrain.cleanup.service;

import com.optibrain.cleanup.dto.OrphanedResourceResponseDTO;
import com.optibrain.cleanup.model.OrphanedResource;
import com.optibrain.cleanup.repository.OrphanedResourceRepository;
import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.MetricQuery;
import com.optibrain.cloud.port.ResourceQuery;
import com.optibrain.common.context.TenantContext;
import com.optibrain.config.service.TenantConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Detects and reclaims resources that are billed but not used.
 *
 * <p>Discovery and deletion both go through {@link CloudProviderPort}. An earlier version
 * of this class opened its own EC2, ELB and CloudWatch clients from per-tenant
 * credentials, which meant that (a) the sandbox endpoint override did not apply, so a
 * sandboxed deployment could call real AWS, and (b) the same four resource scans existed
 * here and in the provider adapter and could disagree. Both problems are structural
 * consequences of bypassing the port, and both disappear by not bypassing it.
 *
 * <p>Deletion is expressed as a {@link ResourceAction} rather than a direct SDK call, so
 * the dry-run interlock and the protected-tag guard apply to cleanup exactly as they do
 * to any other remediation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

    /** A resource with no traffic for this long is considered idle. */
    private static final Duration IDLE_WINDOW = Duration.ofDays(7);

    private static final Map<String, ResourceType> CLEANABLE_TYPES = Map.of(
            "EBS_VOLUME", ResourceType.EBS_VOLUME,
            "ELASTIC_IP", ResourceType.ELASTIC_IP,
            "SNAPSHOT", ResourceType.EBS_SNAPSHOT,
            "LOAD_BALANCER", ResourceType.LOAD_BALANCER);

    private final CloudProviderPort cloudProvider;
    private final TenantConfigurationService configService;
    private final OrphanedResourceRepository orphanedResourceRepository;

    public List<OrphanedResourceResponseDTO> detectOrphanedResources() {
        String tenantId = TenantContext.getTenantId();
        try {
            var config = configService.getConfiguration(tenantId);

            List<OrphanedResource> found = new ArrayList<>();
            found.addAll(detectUnattachedVolumes(tenantId, config.getEbsUnusedDaysThreshold()));
            found.addAll(detectUnattachedIps(tenantId));
            found.addAll(detectOldSnapshots(tenantId, config.getSnapshotOldDaysThreshold()));
            found.addAll(detectIdleLoadBalancers(tenantId));

            orphanedResourceRepository.saveAll(found);
            log.debug("Detected {} orphaned resources for tenant {}", found.size(), tenantId);
            return found.stream().map(this::mapToDTO).toList();
        } catch (Exception e) {
            log.error("Failed to detect orphaned resources for tenant {}: {}", tenantId, e.getMessage());
            // Serving the last known good set beats serving nothing: an empty list reads
            // as "nothing to clean up", which is a materially different claim.
            return orphanedResourceRepository.findByTenantId(tenantId).stream()
                    .map(this::mapToDTO).toList();
        }
    }

    private List<OrphanedResource> detectUnattachedVolumes(String tenantId, int daysThreshold) {
        List<OrphanedResource> result = new ArrayList<>();
        for (CloudResource volume : cloudProvider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME))) {
            int attachments = parseInt(volume.spec("attachmentCount"));
            if (attachments > 0) {
                continue;
            }
            if (ageInDays(volume.createdAt()) < daysThreshold) {
                continue;
            }
            result.add(toEntity(tenantId, volume, "EBS_VOLUME",
                    volume.monthlyCost() == null ? 0.0 : volume.monthlyCost()));
        }
        return result;
    }

    private List<OrphanedResource> detectUnattachedIps(String tenantId) {
        List<OrphanedResource> result = new ArrayList<>();
        for (CloudResource ip : cloudProvider.discover(ResourceQuery.ofTypes(ResourceType.ELASTIC_IP))) {
            if ("true".equalsIgnoreCase(ip.spec("associated"))) {
                continue;
            }
            result.add(toEntity(tenantId, ip, "ELASTIC_IP",
                    ip.monthlyCost() == null ? 0.0 : ip.monthlyCost()));
        }
        return result;
    }

    private List<OrphanedResource> detectOldSnapshots(String tenantId, int daysThreshold) {
        List<OrphanedResource> result = new ArrayList<>();
        for (CloudResource snapshot : cloudProvider.discover(ResourceQuery.ofTypes(ResourceType.EBS_SNAPSHOT))) {
            if (ageInDays(snapshot.createdAt()) < daysThreshold) {
                continue;
            }
            result.add(toEntity(tenantId, snapshot, "SNAPSHOT",
                    snapshot.monthlyCost() == null ? 0.0 : snapshot.monthlyCost()));
        }
        return result;
    }

    /**
     * An idle load balancer is one that served no requests over the idle window.
     *
     * <p>No datapoints is treated as idle: a load balancer with no telemetry is not a
     * load balancer carrying traffic. A load balancer whose telemetry could not be
     * retrieved at all is skipped instead: absence caused by a failed call must not be
     * read as evidence of idleness.
     */
    private List<OrphanedResource> detectIdleLoadBalancers(String tenantId) {
        List<OrphanedResource> result = new ArrayList<>();
        Instant now = Instant.now();
        for (CloudResource lb : cloudProvider.discover(ResourceQuery.ofTypes(ResourceType.LOAD_BALANCER))) {
            String lbName = lb.specOrDefault("name", lb.id());
            var series = cloudProvider.metrics(MetricQuery.forLoadBalancer(
                    lbName, CloudProviderPort.REQUEST_COUNT,
                    now.minus(IDLE_WINDOW), now, IDLE_WINDOW));

            if (series.status() == com.optibrain.cloud.model.DataStatus.UNAVAILABLE) {
                continue;
            }
            double totalRequests = series.points().stream().mapToDouble(p -> p.average()).sum();
            if (totalRequests <= 0.0) {
                result.add(toEntity(tenantId, lb, "LOAD_BALANCER",
                        lb.monthlyCost() == null ? 0.0 : lb.monthlyCost()));
            }
        }
        return result;
    }

    /**
     * Reclaim a resource previously reported as orphaned.
     *
     * <p>Dry-run and protected-resource policy are enforced by the provider, not here,
     * so this method cannot bypass them.
     *
     * @return a human-readable outcome for the audit trail
     */
    public String executeCleanup(String resourceId, String resourceType, boolean dryRun) {
        String tenantId = TenantContext.getTenantId();
        ResourceType type = CLEANABLE_TYPES.get(resourceType.toUpperCase(Locale.ROOT));
        if (type == null) {
            return "FAILED_UNKNOWN_TYPE";
        }

        ActionType action = switch (type) {
            case EBS_VOLUME -> ActionType.DELETE_VOLUME;
            case ELASTIC_IP -> ActionType.RELEASE_ELASTIC_IP;
            case EBS_SNAPSHOT -> ActionType.DELETE_SNAPSHOT;
            case LOAD_BALANCER -> ActionType.DELETE_NETWORK_RESOURCE;
            default -> null;
        };
        if (action == null) {
            return "FAILED_UNKNOWN_TYPE";
        }

        ActionResult result = cloudProvider.execute(ResourceAction.of(
                action, resourceId, dryRun || cloudProvider.isSandboxed(), "orphan cleanup"));

        // A dry-run reports success yet applies nothing; marking the orphan resolved then
        // would claim a cleanup that never happened.
        if (result.applied()) {
            orphanedResourceRepository.findByTenantId(tenantId).stream()
                    .filter(res -> res.getResourceId().equals(resourceId))
                    .forEach(res -> {
                        res.setResolved(true);
                        orphanedResourceRepository.save(res);
                    });
        }
        return (result.success() ? "SUCCESS" : "FAILED") + ": " + result.message();
    }

    private OrphanedResource toEntity(String tenantId, CloudResource resource,
                                      String legacyTypeName, double monthlyCost) {
        OrphanedResource entity = OrphanedResource.builder()
                .resourceId(resource.id())
                .resourceType(legacyTypeName)
                .region(resource.region())
                .estimatedMonthlyCost(monthlyCost)
                .resolved(false)
                .build();
        entity.setTenantId(tenantId);
        return entity;
    }

    private int ageInDays(Instant createdAt) {
        if (createdAt == null) {
            // Unknown age is not evidence of being old enough to delete.
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(createdAt, Instant.now());
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private OrphanedResourceResponseDTO mapToDTO(OrphanedResource entity) {
        return new OrphanedResourceResponseDTO(
                entity.getResourceId(),
                entity.getResourceType(),
                entity.getRegion(),
                entity.getEstimatedMonthlyCost(),
                entity.isResolved(),
                entity.getCreatedAt()
        );
    }
}