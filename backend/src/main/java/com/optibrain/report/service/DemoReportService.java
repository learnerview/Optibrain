package com.optibrain.report.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo implementation of ReportService
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoReportService implements ReportService {

    @Override
    public Map<String, Object> getMonthlyReport() {
        log.info("[DEMO] Generating monthly report");
        
        Map<String, Object> report = new HashMap<>();
        report.put("month", "January 2026");
        report.put("totalCost", 128450);
        report.put("previousMonthCost", 125600);
        report.put("costChange", 2.27);
        report.put("totalSavings", 13000);
        report.put("savingsOpportunities", 2);
        report.put("topService", "EC2");
        report.put("topServiceCost", 62400);
        report.put("anomaliesDetected", 1);
        report.put("optimizationsApplied", 0);
        report.put("generatedAt", LocalDateTime.now().toString());
        
        return report;
    }
}
