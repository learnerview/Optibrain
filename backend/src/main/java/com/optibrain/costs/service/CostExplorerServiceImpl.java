package com.optibrain.costs.service;

import com.optibrain.cloud.model.CostReport;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.CostQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serves the cost-explorer API shape from real Cost Explorer data.
 *
 * <p>This previously returned {@code 12450.50} as a total and {@code Math.random() * 100}
 * per day. Numbers that look authoritative but are invented are worse than an empty
 * response: nobody can tell a plausible figure from a real one, so a user will act on
 * fiction. Every figure here now comes from {@link CloudProviderPort}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CostExplorerServiceImpl implements CostExplorerService {

    private final CloudProviderPort cloudProvider;

    @Override
    public Map<String, Object> getCostData(LocalDate from, LocalDate to, String service, String region) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate end = to != null ? to : LocalDate.now();

        CostReport report = cloudProvider.costReport(CostQuery.lastDays(30));

        Map<String, Object> costData = new LinkedHashMap<>();
        costData.put("from", start.toString());
        costData.put("to", end.toString());
        costData.put("service", service != null ? service : "ALL");
        costData.put("region", region != null ? region : "ALL");
        costData.put("totalCost", report.total().doubleValue());
        costData.put("currency", report.currency());
        costData.put("costByService", report.byService());
        costData.put("costByRegion", report.byRegion());
        costData.put("dailyCosts", dailyBuckets(report));
        costData.put("serviceBreakdown", serviceBreakdown(report));
        costData.put("available", !report.byDay().isEmpty());

        return costData;
    }

    @Override
    public List<Map<String, Object>> getDailyCosts(LocalDate from, LocalDate to) {
        CostReport report = cloudProvider.costReport(CostQuery.lastDays(30));
        return dailyBuckets(report);
    }

    @Override
    public List<Map<String, Object>> getServiceBreakdown() {
        CostReport report = cloudProvider.costReport(CostQuery.lastDays(30));
        return serviceBreakdown(report);
    }

    private List<Map<String, Object>> dailyBuckets(CostReport report) {
        List<Map<String, Object>> daily = new ArrayList<>();
        for (CostReport.TimeBucket bucket : report.byDay()) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", bucket.start().atZone(ZoneOffset.UTC).toLocalDate().toString());
            day.put("cost", bucket.amount().doubleValue());
            daily.add(day);
        }
        return daily;
    }

    /** Spend per service with its share of the total, biggest first. */
    private List<Map<String, Object>> serviceBreakdown(CostReport report) {
        List<Map<String, Object>> services = new ArrayList<>();
        BigDecimal total = report.total();

        report.byService().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("service", entry.getKey());
                    row.put("cost", entry.getValue().doubleValue());
                    row.put("percentage", total.signum() == 0 ? 0.0
                            : entry.getValue().divide(total, 4, java.math.RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue());
                    services.add(row);
                });
        return services;
    }
}