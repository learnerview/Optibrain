package com.optibrain.cloud.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Aggregated spend over a period, grouped by the dimensions the product actually
 * needs (service, region, account).
 *
 * <p>Grouping happens server-side once, in the provider, so that every consumer -
 * dashboard, allocation report, anomaly detector, recommendation engine - reads the
 * same numbers instead of each re-querying and re-grouping Cost Explorer.
 *
 * @param start inclusive start of the period
 * @param end exclusive end of the period
 * @param granularity DAILY or MONTHLY
 * @param currency ISO currency code
 * @param total total spend in the period
 * @param byService spend grouped by AWS service
 * @param byRegion spend grouped by region
 * @param byAccount spend grouped by linked account id
 * @param byDay spend per day, oldest first, for trend rendering
 * @param byServiceDaily spend per day per service, so per-service anomalies are detected
 *                       on a service's own series rather than on the account total
 * @param status whether the report is measured spend or must not be read as zero
 */
public record CostReport(
        Instant start,
        Instant end,
        Granularity granularity,
        String currency,
        BigDecimal total,
        Map<String, BigDecimal> byService,
        Map<String, BigDecimal> byRegion,
        Map<String, BigDecimal> byAccount,
        List<TimeBucket> byDay,
        Map<String, List<TimeBucket>> byServiceDaily,
        DataStatus status
) {

    public CostReport {
        byService = byService == null ? Map.of() : Map.copyOf(byService);
        byRegion = byRegion == null ? Map.of() : Map.copyOf(byRegion);
        byAccount = byAccount == null ? Map.of() : Map.copyOf(byAccount);
        byDay = byDay == null ? List.of() : List.copyOf(byDay);
        byServiceDaily = byServiceDaily == null ? Map.of() : Map.copyOf(byServiceDaily);
        status = status == null ? DataStatus.AVAILABLE : status;
    }

    /** True only when the report is measured spend, not a failure fallback. */
    public boolean isUsable() {
        return status == DataStatus.AVAILABLE;
    }

    /**
     * A report produced when Cost Explorer could not be reached. Its status is
     * {@link DataStatus#UNAVAILABLE}: the {@link BigDecimal#ZERO} total here is a
     * placeholder and a consumer must not present it as "the account spent nothing".
     */
    public static CostReport empty(Instant start, Instant end) {
        return new CostReport(start, end, Granularity.DAILY, "USD",
                BigDecimal.ZERO, Map.of(), Map.of(), Map.of(), List.of(), Map.of(),
                DataStatus.UNAVAILABLE);
    }

    /**
     * Cost Explorer supports only DAILY, MONTHLY and HOURLY granularity; any other
     * value fails the API call, so non-API levels are deliberately not modelled.
     */
    public enum Granularity {
        DAILY,
        MONTHLY
    }

    /**
     * Spend for a single day.
     *
     * @param date day starting at {@code start}
     * @param amount spend for that day
     * @param estimated true when the day is incomplete or was forecast rather than billed
     */
    public record TimeBucket(Instant start, BigDecimal amount, boolean estimated) {
    }
}