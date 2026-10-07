package com.optibrain.analytics.service;

import com.optibrain.cloud.model.CostReport;
import com.optibrain.cloud.model.CostReport.TimeBucket;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.CostQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects cost anomalies from the daily spend series.
 *
 * <p>This previously returned two invented anomalies on every call - "Cost spike in EC2,
 * 45% increase detected" and "Unusual API calls, 250% increase" - which meant the
 * anomaly count never changed and never corresponded to the account.
 *
 * <p>Anomalies are now computed with a rolling median and a median absolute deviation
 * rather than a fixed percentage, so a service whose spend is naturally volatile is not
 * flagged every single day. With too little history the answer is "cannot determine"
 * rather than a guess.
 *
 * <p>Each service is scored against its own daily series, not the account total: a spike
 * in a small service would otherwise be invisible next to a large flat one, and the
 * account total was previously labelled with every service name, duplicating the same
 * account-level detections under each label.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyServiceImpl implements AnomalyService {

    private final CloudProviderPort cloudProvider;

    /** Below this many days, a baseline cannot be established. */
    private static final int MINIMUM_HISTORY = 7;

    /** A point is anomalous when it exceeds median + (z * MAD-scaled deviation). */
    private static final double DEVIATION_MULTIPLIER = 3.0;

    @Override
    public Map<String, Object> getAnomalies() {
        Map<String, Object> response = new LinkedHashMap<>();

        CostReport report;
        try {
            report = cloudProvider.costReport(CostQuery.lastDays(60));
        } catch (Exception e) {
            log.warn("Cost data unavailable for anomaly detection: {}", e.getMessage());
            return unavailable(response, "Cost data is not available: " + e.getMessage());
        }

        if (!report.isUsable()) {
            return unavailable(response, "Cost data could not be retrieved from Cost Explorer");
        }

        List<TimeBucket> daily = report.byDay();
        if (daily.size() < MINIMUM_HISTORY) {
            response.put("anomalies", List.of());
            response.put("count", 0);
            response.put("status", "INSUFFICIENT_DATA");
            response.put("reason", "Need at least " + MINIMUM_HISTORY + " days of daily spend; have "
                    + daily.size() + ".");
            response.put("timestamp", Instant.now().toString());
            return response;
        }

        List<Map<String, Object>> anomalies = new ArrayList<>();
        for (Map.Entry<String, List<TimeBucket>> entry : report.byServiceDaily().entrySet()) {
            // A service with too little history to establish a baseline is skipped
            // outright rather than scored against a guess.
            if (entry.getValue().size() < MINIMUM_HISTORY) {
                continue;
            }
            anomaliesFromSeries(entry.getValue(), entry.getKey()).forEach(anomalies::add);
        }

        response.put("anomalies", anomalies);
        response.put("count", anomalies.size());
        response.put("status", anomalies.isEmpty() ? "NOMINAL" : "ANOMALIES_DETECTED");
        response.put("method", "rolling median +/- " + DEVIATION_MULTIPLIER + " scaled MAD");
        response.put("daysAnalysed", daily.size());
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    private Map<String, Object> unavailable(Map<String, Object> response, String reason) {
        response.put("anomalies", List.of());
        response.put("count", 0);
        response.put("status", "UNAVAILABLE");
        response.put("reason", reason);
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    private List<Map<String, Object>> anomaliesFromSeries(List<TimeBucket> series, String service) {
        List<Map<String, Object>> found = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (TimeBucket bucket : series) {
            values.add(bucket.amount().doubleValue());
        }

        double median = median(values);
        double mad = medianAbsoluteDeviation(values, median);
        // A perfectly flat series has zero deviation; fall back to a small fraction of the
        // median so that "unchanged spend" is not reported as anomalous.
        double threshold = DEVIATION_MULTIPLIER * (mad > 0 ? mad * 1.4826 : Math.abs(median) * 0.05);
        if (threshold <= 0) {
            return found;
        }

        for (TimeBucket bucket : series) {
            double value = bucket.amount().doubleValue();
            if (value <= median + threshold) {
                continue;
            }
            double increasePct = median == 0 ? 0.0 : ((value - median) / median) * 100.0;
            Map<String, Object> anomaly = new LinkedHashMap<>();
            anomaly.put("title", "Cost spike in " + service);
            anomaly.put("description", String.format(
                    "%.2f on %s against a %.2f baseline (%.1f%% above the rolling median)",
                    value, bucket.start(), median, increasePct));
            anomaly.put("severity", increasePct >= DEVIATION_MULTIPLIER * 100 ? "HIGH" : "MEDIUM");
            anomaly.put("service", service);
            anomaly.put("date", bucket.start().toString());
            anomaly.put("actualCost", value);
            anomaly.put("expectedCost", round(median));
            anomaly.put("timestamp", bucket.start().toString());
            anomaly.put("status", "ACTIVE");
            found.add(anomaly);
        }
        return found;
    }

    private double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int n = sorted.size();
        if (n == 0) {
            return 0.0;
        }
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private double medianAbsoluteDeviation(List<Double> values, double median) {
        List<Double> deviations = new ArrayList<>(values.size());
        for (double v : values) {
            deviations.add(Math.abs(v - median));
        }
        return median(deviations);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
