package com.optibrain.report.service;

import com.optibrain.cloud.model.CostReport;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.CostQuery;
import com.optibrain.cloud.port.ResourceQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the monthly report from measured spend.
 *
 * <p>This previously emitted a fixed summary - totalCost 8920.30, 145 resources, 8
 * optimizations applied - regardless of what the account actually contained. A monthly
 * report is a document someone may act on or show to a finance team, so inventing its
 * numbers is worse than returning a report that is genuinely empty.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final CloudProviderPort cloudProvider;

    @Override
    public Map<String, Object> getMonthlyReport() {
        CostReport current = cloudProvider.costReport(CostQuery.lastDays(30));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportMonth", LocalDate.now().getMonth().toString());
        report.put("reportYear", LocalDate.now().getYear());
        report.put("generatedDate", LocalDate.now().toString());
        report.put("currency", current.currency());

        int resourceCount = cloudProvider.discover(ResourceQuery.all()).size();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCost", current.total().doubleValue());
        summary.put("resourceCount", resourceCount);
        // Populated only from measured data; absent keys mean "not measured", not zero.
        summary.put("costByRegion", current.byRegion());
        report.put("summary", summary);

        report.put("topCostServices", serviceSpend(current));
        report.put("dailySpend", dailySpend(current));
        report.put("available", current.isUsable());
        report.put("status", current.status().name());

        return report;
    }

    private List<Map<String, Object>> serviceSpend(CostReport report) {
        List<Map<String, Object>> services = new ArrayList<>();
        report.byService().entrySet().stream()
                .sorted(Map.Entry.<String, java.math.BigDecimal>comparingByValue().reversed())
                .forEach(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("service", entry.getKey());
                    row.put("cost", entry.getValue().doubleValue());
                    services.add(row);
                });
        return services;
    }

    private List<Map<String, Object>> dailySpend(CostReport report) {
        List<Map<String, Object>> daily = new ArrayList<>();
        for (CostReport.TimeBucket bucket : report.byDay()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", bucket.start().toString());
            row.put("cost", bucket.amount().doubleValue());
            daily.add(row);
        }
        return daily;
    }
}