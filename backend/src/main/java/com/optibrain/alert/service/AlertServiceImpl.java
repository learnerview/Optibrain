package com.optibrain.alert.service;

import com.optibrain.cloud.model.CostReport;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds alerts from measured account state.
 *
 * <p>This previously returned three fixed alerts - "EC2 costs increased by 45% in the last
 * 24 hours", "3 EC2 instances with &lt;5% CPU utilization detected" - constructed in the
 * constructor and never changing. A user watching an alerts list would reasonably conclude
 * the platform had detected those things about their account.
 *
 * <p>Alerts are now derived on each read from the same measured data the rest of the
 * product uses, and acknowledgement state is the only thing retained.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final CloudProviderPort cloudProvider;

    /**
     * Acknowledged alert keys, keyed by a stable identifier derived from the alert's own
     * content so acknowledgement survives the alert list being recomputed.
     */
    private final Map<String, Boolean> acknowledged = new ConcurrentHashMap<>();

    @Override
    public List<Map<String, Object>> getAllAlerts() {
        List<Map<String, Object>> alerts = derive();
        alerts.forEach(alert -> alert.put("acknowledged",
                acknowledged.getOrDefault(String.valueOf(alert.get("id")), false)));
        return alerts;
    }

    @Override
    public List<Map<String, Object>> getAlertsBySeverity(String severity) {
        return getAllAlerts().stream()
                .filter(alert -> severity.equalsIgnoreCase((String) alert.get("severity")))
                .toList();
    }

    @Override
    public Map<String, Object> acknowledgeAlert(String id) {
        acknowledged.put(id, true);
        return getAllAlerts().stream()
                .filter(alert -> id.equals(alert.get("id")))
                .findFirst()
                .orElse(null);
    }

    private List<Map<String, Object>> derive() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        try {
            alerts.addAll(spendAlerts());
        } catch (Exception e) {
            log.debug("Spend alert derivation skipped: {}", e.getMessage());
        }
        return alerts;
    }

    /**
     * Flags spend that is materially above the trailing average.
     *
     * <p>Returns nothing at all when Cost Explorer has no data, rather than reporting a
     * spike against a zero baseline.
     */
    private List<Map<String, Object>> spendAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        CostReport report = cloudProvider.costReport(CostQuery.lastDays(30));

        if (report.byDay().isEmpty() || report.total().signum() == 0) {
            return alerts;
        }

        var lastSeven = report.byDay().subList(
                Math.max(0, report.byDay().size() - 7), report.byDay().size());
        var previous = report.byDay().subList(
                0, Math.max(0, report.byDay().size() - 7));

        double recent = sum(lastSeven);
        double baseline = sum(previous);
        if (baseline <= 0 || recent <= 0) {
            return alerts;
        }

        double changePct = ((recent - baseline) / baseline) * 100.0;
        if (changePct >= 25.0) {
            alerts.add(alert(
                    "spend-spike",
                    changePct >= 50 ? "HIGH" : "MEDIUM",
                    "Spend increased",
                    String.format("Last 7 days cost %,.2f versus %,.2f for the preceding period (%+.1f%%)",
                            recent, baseline, changePct)));
        }
        return alerts;
    }

    private double sum(java.util.List<CostReport.TimeBucket> buckets) {
        return buckets.stream()
                .mapToDouble(b -> b.amount().doubleValue())
                .sum();
    }

    private Map<String, Object> alert(String id, String severity, String title, String message) {
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("id", id);
        alert.put("severity", severity);
        alert.put("title", title);
        alert.put("message", message);
        alert.put("acknowledged", false);
        alert.put("timestamp", Instant.now().toString());
        return alert;
    }
}