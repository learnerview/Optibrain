package com.optibrain.analytics.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController("analyticsDashboardController")
@RequestMapping("/api/v1/analytics/metrics")
@RequiredArgsConstructor
public class DashboardController {

    @GetMapping("/cost-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCostSummary() {
        String tenantId = TenantContext.getTenantId();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCost", 125840.50);
        summary.put("monthlyCost", 12450.20);
        summary.put("savingsEstimate", 15200.00);
        summary.put("activeProviders", 3);
        summary.put("currency", "USD");
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/cost-trend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCostTrend() {
        Map<String, Object> trend = new HashMap<>();
        trend.put("period", "30d");
        trend.put("daysIncluded", 30);
        
        List<Map<String, Object>> data = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", cal.getTime().toString());
            point.put("cost", 400 + Math.random() * 100);
            point.put("resourceCount", 150);
            point.put("provider", "ALL");
            data.add(point);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        trend.put("data", data);
        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    @GetMapping("/service-breakdown")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServiceBreakdown() {
        Map<String, Object> breakdown = new HashMap<>();
        List<Map<String, Object>> services = new ArrayList<>();
        
        services.add(createService("AWS", "EC2", 45000, 35.8, "Compute"));
        services.add(createService("AWS", "S3", 12000, 9.5, "Storage"));
        services.add(createService("Azure", "VM", 38000, 30.2, "Compute"));
        services.add(createService("GCP", "BigQuery", 15000, 11.9, "Data"));
        
        breakdown.put("services", services);
        breakdown.put("totalCost", 110000);
        breakdown.put("servicesCount", 4);
        
        return ResponseEntity.ok(ApiResponse.success(breakdown));
    }

    private Map<String, Object> createService(String p, String s, double c, double pct, String cat) {
        Map<String, Object> map = new HashMap<>();
        map.put("provider", p);
        map.put("service", s);
        map.put("cost", c);
        map.put("percentage", pct);
        map.put("category", cat);
        return map;
    }
}
