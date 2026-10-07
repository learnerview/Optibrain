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
 */
public record CostQuery(
        Instant start,
        Instant end,
        CostReport.Granularity granularity,
        int forecastDays,
        int topServices
) {

    public CostQuery {
        granularity = granularity == null ? CostReport.Granularity.DAILY : granularity;
    }

    /** The last {@code days} days, which is what every dashboard panel asks for. */
    public static CostQuery lastDays(int days) {
        Instant end = Instant.now();
        return new CostQuery(end.minus(Duration.ofDays(days)), end,
                CostReport.Granularity.DAILY, 0, 10);
    }

    public static CostQuery last30Days() {
        return lastDays(30);
    }

    public Duration period() {
        return Duration.between(start, end);
    }
}