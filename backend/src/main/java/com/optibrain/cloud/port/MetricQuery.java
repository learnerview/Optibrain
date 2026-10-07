package com.optibrain.cloud.port;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * A fully specified telemetry request.
 *
 * <p>CloudWatch metrics are addressed by a namespace plus a set of dimensions, and those
 * differ per service: an EC2 instance is {@code AWS/EC2} + {@code InstanceId}, an
 * application load balancer is {@code AWS/ApplicationELB} + {@code LoadBalancer}, an
 * RDS instance is {@code AWS/RDS} + {@code DBInstanceIdentifier}. Modelling only
 * "resourceId + metric name" forced every caller that cared about anything other than
 * EC2 CPU to construct its own SDK client, which is how four copies of the same
 * CloudWatch query ended up in this codebase.
 *
 * <p>Use the {@code for*} factories for the common cases.
 *
 * @param namespace CloudWatch namespace, for example {@code AWS/EC2}
 * @param dimensions dimension name/value pairs identifying the resource
 * @param metricName CloudWatch metric name
 * @param statistics which statistics to return
 * @param start inclusive start of the window
 * @param end exclusive end of the window
 * @param period aggregation window; CloudWatch constrains this per resolution
 */
public record MetricQuery(
        String namespace,
        Map<String, String> dimensions,
        String metricName,
        Set<String> statistics,
        Instant start,
        Instant end,
        Duration period
) {

    public static final String NAMESPACE_EC2 = "AWS/EC2";
    public static final String NAMESPACE_ELB = "AWS/ApplicationELB";

    private static final Set<String> DEFAULT_STATISTICS = Set.of("Average", "Minimum", "Maximum");

    public MetricQuery {
        dimensions = dimensions == null ? Map.of() : Map.copyOf(dimensions);
        statistics = statistics == null || statistics.isEmpty()
                ? DEFAULT_STATISTICS : Set.copyOf(statistics);
    }

    /** The common case: an EC2 instance metric such as CPUUtilization. */
    public static MetricQuery forEc2Instance(String instanceId, String metricName,
                                             Instant start, Instant end, Duration period) {
        return new MetricQuery(NAMESPACE_EC2, Map.of("InstanceId", instanceId), metricName,
                DEFAULT_STATISTICS, start, end, period);
    }

    /** An application load balancer request count, used for idle-LB detection. */
    public static MetricQuery forLoadBalancer(String loadBalancerName, String metricName,
                                               Instant start, Instant end, Duration period) {
        return new MetricQuery(NAMESPACE_ELB, Map.of("LoadBalancer", loadBalancerName), metricName,
                Set.of("Sum"), start, end, period);
    }
}