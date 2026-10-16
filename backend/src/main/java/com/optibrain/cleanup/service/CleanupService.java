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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

            List<OrphanedResource> reported = reconcile(tenantId, found);
            log.debug("Detected {} orphaned resources for tenant {}", reported.size(), tenantId);
            return reported.stream().map(this::mapToDTO).toList();
        } catch (Exception e) {
            log.error("Failed to detect orphaned resources for tenant {}: {}", tenantId, e.getMessage());
            // Serving the last known good set beats serving nothing: an empty list reads
            // as "nothing to clean up", which is a materially different claim.
            return orphanedResourceRepository.findByTenantId(tenantId).stream()
                    .map(this::mapToDTO).toList();
        }
    }

    /**
     * Merges this scan into the stored inventory instead of inserting a second row for a
     * resource that is already recorded.
     *
     * <p>Identity is the composite (tenant, provider, region, resource type, resource id),
     * so two tenants holding resources with the same id, or the same load balancer name
     * in two regions, are two separate rows. A rediscovered resource has its stored row
     * refreshed and reopened; a stored row this scan no longer reports is closed.
     *
     * <p>Closing on disappearance is the explicit policy: a resource the inventory no
     * longer reports as orphaned is either gone or no longer orphaned, and leaving it
     * open would invite a deletion the scanner no longer stands behind. AWS scanners
     * swallow their own failures and return nothing, so a failed section closes rows
     * too - which is the safe direction: a closed row suppresses cleanup, and the next
     * complete scan reopens the resource.
     *
     * @return the rows representing this scan, in discovery order
     */
    private List<OrphanedResource> reconcile(String tenantId, List<OrphanedResource> found) {
        String provider = cloudProvider.provider();
        Map<String, OrphanedResource> stored = new LinkedHashMap<>();
        for (OrphanedResource row : loadStored(tenantId, provider)) {
            stored.put(identityKey(row), row);
        }

        List<OrphanedResource> current = new ArrayList<>();
        List<OrphanedResource> stale = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (OrphanedResource row : found) {
            row.setProvider(provider);
            String key = identityKey(row);
            seen.add(key);
            OrphanedResource existing = stored.get(key);
            if (existing == null) {
                current.add(row);
                continue;
            }
            existing.setEstimatedMonthlyCost(row.getEstimatedMonthlyCost());
            existing.setResolved(false);
            current.add(existing);
        }
        stored.forEach((key, row) -> {
            if (!seen.contains(key) && !row.isResolved()) {
                row.setResolved(true);
                stale.add(row);
            }
        });

        List<OrphanedResource> changes = new ArrayList<>(current);
        changes.addAll(stale);
        orphanedResourceRepository.saveAll(changes);
        return current;
    }

    /**
     * This tenant's stored rows, with rows written before the provider was part of the
     * identity adopted by the provider reporting them, so an upgraded row matches this
     * scan rather than duplicating it.
     */
    private List<OrphanedResource> loadStored(String tenantId, String provider) {
        List<OrphanedResource> stored = orphanedResourceRepository.findByTenantId(tenantId);
        stored.forEach(row -> {
            if (row.getProvider() == null) {
                row.setProvider(provider);
            }
        });
        return stored;
    }

    private String identityKey(OrphanedResource row) {
        return identityKey(row.getTenantId(), row.getProvider(), row.getRegion(),
                row.getResourceType(), row.getResourceId());
    }

    private String identityKey(String tenantId, String provider, String region,
                               String resourceType, String resourceId) {
        return String.join("|", text(tenantId), text(provider), text(region),
                text(resourceType), text(resourceId));
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private List<OrphanedResource> detectUnattachedVolumes(String tenantId, int daysThreshold) {
        List<OrphanedResource> result = new ArrayList<>();
        for (CloudResource volume : cloudProvider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME))) {
            Integer attachments = parseCount(volume.spec("attachmentCount"));
            // Only an explicit zero proves the volume is unattached. Missing or
            // unparseable metadata is an unknown attachment state, and unknown must not
            // become evidence for reclaiming a volume.
            if (attachments == null || attachments != 0) {
                continue;
            }
            if (ageInDays(volume.createdAt()) < daysThreshold) {
                continue;
            }
            result.add(toEntity(tenantId, volume, "EBS_VOLUME", volume.monthlyCost()));
        }
        return result;
    }

    private List<OrphanedResource> detectUnattachedIps(String tenantId) {
        List<OrphanedResource> result = new ArrayList<>();
        for (CloudResource ip : cloudProvider.discover(ResourceQuery.ofTypes(ResourceType.ELASTIC_IP))) {
            if ("true".equalsIgnoreCase(ip.spec("associated"))) {
                continue;
            }
            result.add(toEntity(tenantId, ip, "ELASTIC_IP", ip.monthlyCost()));
        }
        return result;
    }

    private List<OrphanedResource> detectOldSnapshots(String tenantId, int daysThreshold) {
        List<OrphanedResource> result = new ArrayList<>();
        for (CloudResource snapshot : cloudProvider.discover(ResourceQuery.ofTypes(ResourceType.EBS_SNAPSHOT))) {
            if (ageInDays(snapshot.createdAt()) < daysThreshold) {
                continue;
            }
            result.add(toEntity(tenantId, snapshot, "SNAPSHOT", snapshot.monthlyCost()));
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
                result.add(toEntity(tenantId, lb, "LOAD_BALANCER", lb.monthlyCost()));
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
    public String executeCleanup(String resourceId, String resourceType, String region,
                                 boolean dryRun) {
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
            String provider = cloudProvider.provider();
            String key = identityKey(tenantId, provider, region, resourceType, resourceId);
            loadStored(tenantId, provider).stream()
                    .filter(res -> identityKey(res).equals(key))
                    .forEach(res -> {
                        res.setResolved(true);
                        orphanedResourceRepository.save(res);
                    });
        }
        return (result.success() ? "SUCCESS" : "FAILED") + ": " + result.message();
    }

    private OrphanedResource toEntity(String tenantId, CloudResource resource,
                                      String legacyTypeName, Double monthlyCost) {
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

    /** Attachment count, or {@code null} when the provider reported no usable value. */
    private Integer parseCount(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
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