package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.config.CloudMode;
import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.CostReport;
import com.optibrain.cloud.model.DataStatus;
import com.optibrain.cloud.model.MetricsSeries;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.CostQuery;
import com.optibrain.cloud.port.MetricQuery;
import com.optibrain.cloud.port.ResourceQuery;
import com.optibrain.cloud.port.ResourceScanner;
import com.optibrain.cloud.policy.ProtectionPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.costexplorer.model.DateInterval;
import software.amazon.awssdk.services.costexplorer.model.DimensionValues;
import software.amazon.awssdk.services.costexplorer.model.Expression;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageRequest;
import software.amazon.awssdk.services.costexplorer.model.GetCostAndUsageResponse;
import software.amazon.awssdk.services.costexplorer.model.Group;
import software.amazon.awssdk.services.costexplorer.model.GroupDefinition;
import software.amazon.awssdk.services.costexplorer.model.GroupDefinitionType;
import software.amazon.awssdk.services.costexplorer.model.MetricValue;
import software.amazon.awssdk.services.costexplorer.model.ResultByTime;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The AWS implementation of {@link CloudProviderPort}.
 *
 * <p>There is deliberately no {@code @ConditionalOnProperty} here. This adapter serves
 * both execution targets: in SANDBOX the {@link AwsClientFactory} points it at LocalStack,
 * in AWS at the real endpoints. Gating the bean on {@code cloud.mode=AWS} would leave the
 * sandbox with no provider at all.
 *
 * <p>This class owns three concerns and delegates the fourth. Inventory discovery is
 * delegated to {@link ResourceScanner} implementations, one per resource type, so
 * coverage extends by adding a bean rather than by growing this file. Telemetry, cost
 * reporting and remediation stay here because each is a single coherent algorithm over
 * its service.
 *
 * <p>All SDK clients come from {@link AwsClientFactory}, so region, credentials and
 * endpoint override apply in one place.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AwsCloudProviderAdapter implements CloudProviderPort {

    private final AwsClientFactory clients;
    private final CloudProperties properties;

    /**
     * Every registered scanner.
     *
     * <p>Injected as a list so Spring discovers them all. Adding a resource type means
     * adding a {@code @Component} scanner; this class does not change.
     */
    private final List<ResourceScanner> scanners;

    /** Commitment coverage, refreshed per cost report. */
    private final CommitmentCoverage commitments = new CommitmentCoverage();

    // ------------------------------------------------------------------
    // ProviderDescriptor
    // ------------------------------------------------------------------

    @Override
    public String provider() {
        return "aws";
    }

    @Override
    public String region() {
        return clients.region();
    }

    @Override
    public CloudMode executionTarget() {
        return properties.getMode();
    }

    @Override
    public boolean isSandboxed() {
        return properties.getMode() == CloudMode.SANDBOX;
    }

    // ------------------------------------------------------------------
    // ResourceInventory
    // ------------------------------------------------------------------

    @Override
    public List<CloudResource> discover(ResourceQuery query) {
        List<CloudResource> all = new java.util.ArrayList<>();
        for (ResourceScanner scanner : scanners) {
            all.addAll(scanner.scan(query));
        }
        return all.stream()
                .filter(r -> query.matchesType(r.type()))
                .filter(r -> query.matchesState(r.state()))
                .filter(r -> query.matchesRegion(r.region()))
                .filter(r -> query.matchesTags(r.tags()))
                .limit(query.limit() > 0 ? query.limit() : Long.MAX_VALUE)
                .toList();
    }

    @Override
    public Optional<CloudResource> find(String resourceId) {
        return discover(ResourceQuery.all()).stream()
                .filter(r -> r.id().equals(resourceId))
                .findFirst();
    }

    // ------------------------------------------------------------------
    // TelemetrySource
    // ------------------------------------------------------------------

    @Override
    public MetricsSeries metrics(MetricQuery query) {
        Duration spacing = query.period();
        if (query.metricName() == null || query.dimensions().isEmpty()) {
            return new MetricsSeries(resourceKey(query), query.metricName(), query.namespace(),
                    spacing, List.of(), DataStatus.EMPTY);
        }
        try (var cw = clients.cloudWatch()) {
            List<Dimension> dimensions = query.dimensions().entrySet().stream()
                    .map(e -> Dimension.builder().name(e.getKey()).value(e.getValue()).build())
                    .toList();

            GetMetricStatisticsResponse response = cw.getMetricStatistics(
                    GetMetricStatisticsRequest.builder()
                            .namespace(query.namespace())
                            .metricName(query.metricName())
                            .startTime(query.start())
                            .endTime(query.end())
                            .period((int) query.period().toSeconds())
                            .statistics(query.statistics().stream()
                                    .map(Statistic::fromValue).toArray(Statistic[]::new))
                            .dimensions(dimensions)
                            .build());

            List<MetricsSeries.Point> points = response.datapoints().stream()
                    .map(this::toPoint)
                    .sorted(Comparator.comparing(MetricsSeries.Point::timestamp))
                    .toList();

            // An empty response for a valid window is a genuine absence; a failed call
            // is not, and is distinguished above by the exception path.
            DataStatus status = points.isEmpty() ? DataStatus.EMPTY : DataStatus.AVAILABLE;
            return new MetricsSeries(resourceKey(query), query.metricName(), query.namespace(),
                    spacing, points, status);
        } catch (Exception e) {
            log.warn("No telemetry for {} {}: {}", query.namespace(), query.metricName(),
                    e.getMessage());
            return new MetricsSeries(resourceKey(query), query.metricName(), query.namespace(),
                    spacing, List.of(), DataStatus.UNAVAILABLE);
        }
    }

    /** Identifier the series is filed under; the primary dimension's value. */
    private String resourceKey(MetricQuery query) {
        return query.dimensions().values().stream().findFirst().orElse(null);
    }

    /**
     * CloudWatch returns only the statistics that were requested, so every field is
     * nullable and falls back to the mean rather than to zero. Zero would read as
     * "genuinely idle" and could trigger a spurious rightsizing recommendation.
     */
    private MetricsSeries.Point toPoint(Datapoint dp) {
        double mean = dp.average() != null ? dp.average()
                : dp.sum() != null && dp.sampleCount() != null
                        ? dp.sum() / Math.max(1.0, dp.sampleCount())
                        : 0.0;
        return new MetricsSeries.Point(
                dp.timestamp(),
                mean,
                dp.minimum() != null ? dp.minimum() : mean,
                dp.maximum() != null ? dp.maximum() : mean,
                dp.sampleCount() != null ? dp.sampleCount().longValue() : 0L);
    }

    // ------------------------------------------------------------------
    // CostSource
    // ------------------------------------------------------------------

    @Override
    public CostReport costReport(CostQuery query) {
        try (var ce = clients.costExplorer()) {
            commitments.refresh(ce);

            GetCostAndUsageResponse primary = fetchCostAndUsage(ce, query, "SERVICE");

            Map<String, BigDecimal> byService = new LinkedHashMap<>();
            Map<String, List<CostReport.TimeBucket>> byServiceDaily = new LinkedHashMap<>();
            for (ResultByTime bucket : primary.resultsByTime()) {
                Instant day = startOfDay(bucket.timePeriod());
                for (Group group : bucket.groups()) {
                    String service = keyOrUnattributed(group.keys());
                    byService.merge(service, unblended(group), BigDecimal::add);
                    byServiceDaily.computeIfAbsent(service, k -> new java.util.ArrayList<>())
                            .add(new CostReport.TimeBucket(day, unblended(group), false));
                }
            }
            byServiceDaily.forEach((service, buckets) -> buckets.sort(
                    Comparator.comparing(CostReport.TimeBucket::start)));

            List<CostReport.TimeBucket> byDay = primary.resultsByTime().stream()
                    .map(bucket -> new CostReport.TimeBucket(
                            startOfDay(bucket.timePeriod()),
                            unblendedTotal(bucket.total()),
                            false))
                    .sorted(Comparator.comparing(CostReport.TimeBucket::start))
                    .toList();

            BigDecimal total = primary.resultsByTime().stream()
                    .map(bucket -> unblendedTotal(bucket.total()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return new CostReport(query.start(), query.end(), query.granularity(), "USD",
                    total, byService,
                    groupedTotals(ce, query, "REGION"),
                    groupedTotals(ce, query, "LINKED_ACCOUNT"),
                    byDay, byServiceDaily, DataStatus.AVAILABLE);
        } catch (Exception e) {
            log.warn("Cost Explorer unavailable, returning empty report: {}", e.getMessage());
            return CostReport.empty(query.start(), query.end());
        }
    }

    /** Measured commitment coverage, or null when Cost Explorer cannot be reached. */
    public Double commitmentCoverage() {
        return commitments.isAvailable() ? commitments.totalCoverage() : null;
    }

    private BigDecimal unblendedTotal(Map<String, MetricValue> metrics) {
        MetricValue value = metrics == null ? null : metrics.get("UnblendedCost");
        if (value == null || value.amount() == null) {
            return BigDecimal.ZERO;
        }
        // MetricValue.amount() is a String in the CE wire format.
        try {
            return new BigDecimal(value.amount());
        } catch (NumberFormatException e) {
            log.warn("Unparseable Cost Explorer amount '{}', treating as zero", value.amount());
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal unblended(Group group) {
        return unblendedTotal(group.metrics());
    }

    /** Cost Explorer reports period boundaries as {@code yyyy-MM-dd} strings. */
    private Instant startOfDay(DateInterval interval) {
        if (interval == null || interval.start() == null) {
            return Instant.EPOCH;
        }
        return LocalDate.parse(interval.start()).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Map<String, BigDecimal> groupedTotals(
            software.amazon.awssdk.services.costexplorer.CostExplorerClient ce,
            CostQuery query, String dimensionKey) {
        try {
            GetCostAndUsageResponse response = fetchCostAndUsage(ce, query, dimensionKey);
            Map<String, BigDecimal> totals = new LinkedHashMap<>();
            for (ResultByTime bucket : response.resultsByTime()) {
                for (Group group : bucket.groups()) {
                    totals.merge(keyOrUnattributed(group.keys()), unblended(group), BigDecimal::add);
                }
            }
            return totals;
        } catch (Exception e) {
            log.warn("Cost Explorer grouping by {} unavailable: {}", dimensionKey, e.getMessage());
            return Map.of();
        }
    }

    private String keyOrUnattributed(List<String> keys) {
        return keys == null || keys.isEmpty() ? "Unattributed" : keys.get(0);
    }

    /**
     * One Cost Explorer round trip.
     *
     * <p>Cost Explorer is the only source of authoritative spend, and it is also the
     * slowest AWS API, so the number of round trips is kept to a minimum and each is
     * isolated: losing the region breakdown must not lose the service breakdown.
     *
     * @param dimensionKey a Cost Explorer dimension to group by, or null for a total
     */
    private GetCostAndUsageResponse fetchCostAndUsage(
            software.amazon.awssdk.services.costexplorer.CostExplorerClient ce,
            CostQuery query, String dimensionKey) {
        var request = GetCostAndUsageRequest.builder()
                .timePeriod(DateInterval.builder()
                        .start(query.start().atZone(ZoneOffset.UTC).toLocalDate().toString())
                        .end(query.end().atZone(ZoneOffset.UTC).toLocalDate().toString())
                        .build())
                .granularity(query.granularity().name())
                .metrics("UnblendedCost");

        if (dimensionKey != null) {
            request.groupBy(GroupDefinition.builder()
                    .type(GroupDefinitionType.DIMENSION)
                    .key(dimensionKey)
                    .build());
        }
        applyFilters(request, query);
        return ce.getCostAndUsage(request.build());
    }

    /**
     * Restricts the report to the service and region named on the query.
     *
     * <p>Cost Explorer offers one effective filter per dimension, so two named filters
     * are combined with an AND rather than appended. A service or region other than the
     * caller's own simply yields an empty (but usable) report; it is the caller's data,
     * not the API's, so a valid value for someone else's account is identical to zero
     * spend here.
     */
    private void applyFilters(GetCostAndUsageRequest.Builder request, CostQuery query) {
        String service = query.service();
        String region = query.region();
        DimensionValues serviceFilter = service == null ? null
                : DimensionValues.builder().key("SERVICE").values(service).build();
        DimensionValues regionFilter = region == null ? null
                : DimensionValues.builder().key("REGION").values(region).build();

        if (serviceFilter != null && regionFilter != null) {
            request.filter(Expression.builder()
                    .and(Expression.builder().dimensions(serviceFilter).build(),
                            Expression.builder().dimensions(regionFilter).build())
                    .build());
        } else if (serviceFilter != null) {
            request.filter(Expression.builder().dimensions(serviceFilter).build());
        } else if (regionFilter != null) {
            request.filter(Expression.builder().dimensions(regionFilter).build());
        }
    }

    @Override
    public Double monthlyCostOf(String resourceId) {
        return find(resourceId).map(CloudResource::monthlyCost).orElse(null);
    }

    // ------------------------------------------------------------------
    // RemediationExecutor
    // ------------------------------------------------------------------

    @Override
    public ActionResult execute(ResourceAction action) {
        boolean dryRun = action.dryRun() || properties.isDryRun();

        // Protected-tag and stateful-resource guards live here, at the only place a
        // mutation can happen, so no service can bypass them.
        Optional<CloudResource> target = find(action.resourceId());
        if (target.isPresent() && target.get().protectedResource()
                && action.type().isDestructive()) {
            return ActionResult.rejected(action,
                    "Resource is tagged " + ProtectionPolicy.TAG + "="
                            + ProtectionPolicy.VALUE + " and refuses destructive actions");
        }

        // Fail closed when discovery cannot verify the target. A throttled or failing
        // describe makes the inventory empty, and running a destructive call on that
        // basis would mean executing against a resource the guards never saw.
        if (target.isEmpty() && !dryRun) {
            return ActionResult.rejected(action,
                    "Target resource could not be found in the inventory; refusing to apply "
                            + "a change that cannot be verified");
        }

        if (dryRun) {
            return ActionResult.simulated(action,
                    "Would " + describe(action) + " (dry-run: no change applied)",
                    projectedSavings(action));
        }

        try (var ec2 = clients.ec2()) {
            return switch (action.type()) {
                case STOP_INSTANCE -> {
                    ec2.stopInstances(b -> b.instanceIds(action.resourceId()));
                    yield ActionResult.succeeded(action, "Stopped " + action.resourceId(),
                            projectedSavings(action));
                }
                case START_INSTANCE -> {
                    ec2.startInstances(b -> b.instanceIds(action.resourceId()));
                    yield ActionResult.succeeded(action, "Started " + action.resourceId(), null);
                }
                case TERMINATE_INSTANCE -> {
                    ec2.terminateInstances(b -> b.instanceIds(action.resourceId()));
                    yield ActionResult.succeeded(action, "Terminated " + action.resourceId(),
                            projectedSavings(action));
                }
                case DELETE_VOLUME -> {
                    ec2.deleteVolume(b -> b.volumeId(action.resourceId()));
                    yield ActionResult.succeeded(action, "Deleted volume " + action.resourceId(),
                            projectedSavings(action));
                }
                case DELETE_SNAPSHOT -> {
                    ec2.deleteSnapshot(b -> b.snapshotId(action.resourceId()));
                    yield ActionResult.succeeded(action, "Deleted snapshot " + action.resourceId(),
                            projectedSavings(action));
                }
                case RELEASE_ELASTIC_IP -> {
                    String allocationId = action.resourceId() != null
                            ? action.resourceId() : requireParameter(action, "allocationId");
                    if (allocationId == null) {
                        yield ActionResult.rejected(action,
                                "allocationId is required to release an Elastic IP");
                    }
                    ec2.releaseAddress(b -> b.allocationId(allocationId));
                    yield ActionResult.succeeded(action, "Released " + allocationId,
                            projectedSavings(action));
                }
                case APPLY_TAGS -> {
                    String key = requireParameter(action, "key");
                    String value = requireParameter(action, "value");
                    if (key == null || value == null) {
                        yield ActionResult.rejected(action, "key and value are required to apply tags");
                    }
                    if (ProtectionPolicy.TAG.equals(key)) {
                        yield ActionResult.rejected(action,
                                "The " + ProtectionPolicy.TAG + " tag is managed by policy and "
                                        + "cannot be applied through remediation");
                    }
                    ec2.createTags(b -> b.resources(action.resourceId())
                            .tags(t -> t.key(key).value(value)));
                    yield ActionResult.succeeded(action,
                            "Applied " + key + "=" + value + " to " + action.resourceId(), null);
                }
                case SCALE_GROUP -> {
                    String desired = requireParameter(action, "desiredCapacity");
                    if (desired == null) {
                        yield ActionResult.rejected(action,
                                "desiredCapacity is required to scale a group");
                    }
                    clients.autoScaling().updateAutoScalingGroup(b -> b
                            .autoScalingGroupName(action.resourceId())
                            .desiredCapacity(Integer.parseInt(desired)));
                    yield ActionResult.succeeded(action,
                            "Scaled " + action.resourceId() + " to " + desired, null);
                }
                case RESIZE_INSTANCE -> {
                    String instanceType = requireParameter(action, "instanceType");
                    if (instanceType == null) {
                        yield ActionResult.rejected(action,
                                "instanceType is required to resize an instance");
                    }
                    ec2.modifyInstanceAttribute(b -> b
                            .instanceId(action.resourceId())
                            .instanceType(b2 -> b2.value(instanceType)));
                    yield ActionResult.succeeded(action,
                            "Requested " + instanceType + " for " + action.resourceId(), null);
                }
                default -> {
                    yield ActionResult.rejected(action,
                            "Action " + action.type() + " has no AWS implementation");
                }
            };
        } catch (Exception e) {
            log.warn("Action {} on {} failed: {}", action.type(), action.resourceId(),
                    e.getMessage());
            return ActionResult.failed(action, e);
        }
    }

    private String requireParameter(ResourceAction action, String key) {
        String value = action.parameter(key);
        return value == null || value.isBlank() ? null : value;
    }

    private String describe(ResourceAction action) {
        return switch (action.type()) {
            case STOP_INSTANCE -> "stop " + action.resourceId();
            case START_INSTANCE -> "start " + action.resourceId();
            case TERMINATE_INSTANCE -> "terminate " + action.resourceId();
            case DELETE_VOLUME -> "delete volume " + action.resourceId();
            case DELETE_SNAPSHOT -> "delete snapshot " + action.resourceId();
            case RELEASE_ELASTIC_IP -> "release address " + action.resourceId();
            case APPLY_TAGS -> "tag " + action.resourceId();
            case SCALE_GROUP -> "scale " + action.resourceId();
            case RESIZE_INSTANCE -> "resize " + action.resourceId();
            default -> action.type() + " " + action.resourceId();
        };
    }

    /**
     * Saving attributable to the action.
     *
     * <p>Only an action that removes or pauses a cost has a saving: stopping, deleting
     * or releasing a resource relinquishes its billed cost. An action that starts,
     * scales or tags a resource increases or reallocates spend, and reporting a saving
     * there would be fabricated. Uses attributed cost when it exists; otherwise the
     * saving is zero rather than estimated, because a list-price figure beside a real
     * cost figure invites the reader to treat them as comparable.
     */
    private Double projectedSavings(ResourceAction action) {
        if (!switch (action.type()) {
            case STOP_INSTANCE, TERMINATE_INSTANCE, DELETE_VOLUME, DELETE_SNAPSHOT,
                    RELEASE_ELASTIC_IP -> true;
            default -> false;
        }) {
            return null;
        }
        Double monthly = monthlyCostOf(action.resourceId());
        return monthly == null ? 0.0 : monthly;
    }
}