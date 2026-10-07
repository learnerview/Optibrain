package com.optibrain.cloud.port;

import com.optibrain.cloud.model.CostReport;

import java.time.Duration;
import java.time.Instant;

/**
 * Criteria for a cost lookup.
 *
 * @param start inclusive start
 * @param end exclusive end
 * @param granularity grouping level
 * @param forecastDays when positive, also produce a forecast this many days forward
 * @param topServices how many services to include in a "top N" breakdown; 0 means all
 * @param service when set, restrict the report to one AWS service
 * @param region when set, restrict the report to one AWS region
 */
public record CostQuery(
        Instant start,
        Instant end,
        CostReport.Granularity granularity,
        int forecastDays,
        int topServices,
        String service,
        String region
) {

    public CostQuery {
        granularity = granularity == null ? CostReport.Granularity.DAILY : granularity;
        service = service == null || service.isBlank() ? null : service.trim();
        region = region == null || region.isBlank() ? null : region.trim();
    }

    /** The last {@code days} days, which is what every dashboard panel asks for. */
    public static CostQuery lastDays(int days) {
        Instant end = Instant.now();
        return new CostQuery(end.minus(Duration.ofDays(days)), end,
                CostReport.Granularity.DAILY, 0, 10, null, null);
    }

    public static CostQuery last30Days() {
        return lastDays(30);
    }

    /**
     * A fully specified window; the form honored by the explorer endpoint.
     *
     * @param service AWS service name to filter by, or null for all services
     * @param region AWS region to filter by, or null for all regions
     */
    public static CostQuery range(Instant start, Instant end, CostReport.Granularity granularity,
                                  String service, String region) {
        return new CostQuery(start, end, granularity, 0, 10, service, region);
    }

    public Duration period() {
        return Duration.between(start, end);
    }
}