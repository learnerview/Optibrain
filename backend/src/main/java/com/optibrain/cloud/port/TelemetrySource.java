package com.optibrain.cloud.port;

import com.optibrain.cloud.model.DataStatus;
import com.optibrain.cloud.model.MetricsSeries;

import java.time.Duration;
import java.time.Instant;

/**
 * Read access to utilisation telemetry.
 *
 * <p>Idle and rightsizing decisions are only as good as this data, so implementations
 * are expected to return a series that is never a fabricated one. The status on the
 * series distinguishes three honest states: {@link DataStatus#AVAILABLE} (measured),
 * {@link DataStatus#EMPTY} (the source returned no observations) and
 * {@link DataStatus#UNAVAILABLE} (the data could not be retrieved, so absence must not
 * be read as zero or idle).
 */
public interface TelemetrySource {

    /** Canonical metric names, so callers are not tied to provider naming. */
    String CPU_UTILIZATION = "CPUUtilization";
    String NETWORK_IN = "NetworkIn";
    String NETWORK_OUT = "NetworkOut";
    String DISK_READ = "DiskReadBytes";
    String DISK_WRITE = "DiskWriteBytes";
    String EBS_READ = "EBSReadBytesPerOp";
    String EBS_WRITE = "EBSWriteBytesPerOp";
    String REQUEST_COUNT = "RequestCount";
    String GPU_UTILIZATION = "GPUUtilization";

    /**
     * Fetch a fully specified metric series.
     *
     * <p>This is the general form; {@link #ec2InstanceMetrics} is the common shorthand.
     *
     * @param query namespace, dimensions, metric and window
     * @return the series, empty when no telemetry exists
     */
    MetricsSeries metrics(MetricQuery query);

    /**
     * Fetch an EC2 instance metric.
     *
     * @param resourceId instance id
     * @param metric one of the canonical EC2 metric constants
     * @param start inclusive start
     * @param end exclusive end
     * @param period aggregation window; for example {@code PT1H}
     * @return the series, empty when no telemetry exists
     */
    default MetricsSeries ec2InstanceMetrics(String resourceId, String metric,
                                             Instant start, Instant end, Duration period) {
        if (resourceId == null || metric == null) {
            return new MetricsSeries(resourceId, metric, "None",
                    period, java.util.List.of(), DataStatus.EMPTY);
        }
        return metrics(MetricQuery.forEc2Instance(resourceId, metric, start, end, period));
    }
}