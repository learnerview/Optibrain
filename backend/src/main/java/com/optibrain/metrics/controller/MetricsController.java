package com.optibrain.metrics.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.analytics.service.AnalyticsService;
import com.optibrain.analytics.model.FinancialReport;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/cost-summary")
    public ApiResponse<Map<String, Object>> getCostSummary() {
        // Fetch real report or use latest
        var report = analyticsService.generateReport("monthly");
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCost", report != null ? report.currentCost() : 0.0);
        summary.put("monthlyCost", report != null ? report.forecastedCost() : 0.0);
        summary.put("savingsEstimate", report != null ? report.potentialSavings() : 0.0);
        summary.put("activeProviders", 1);
        summary.put("currency", "USD");
        
        return ApiResponse.success(summary);
    }
    
    @GetMapping("/cost-trend")
    public ApiResponse<Map<String, Object>> getCostTrend(
            @RequestParam(defaultValue = "30") int daysBack,
            @RequestParam(defaultValue = "daily") String granularity) {
            
        List<Map<String, Object>> trends = new ArrayList<>();
        // Mock trend data for now - in real implementation this comes from AnalyticsService
        LocalDate now = LocalDate.now();
        for (int i = 0; i < daysBack; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", now.minusDays(daysBack - i).toString());
            point.put("cost", 50.0 + Math.random() * 20);
            point.put("resourceCount", 10 + (int)(Math.random() * 5));
            point.put("provider", "AWS");
            trends.add(point);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("period", daysBack + "d");
        response.put("daysIncluded", daysBack);
        response.put("data", trends);
        
        return ApiResponse.success(response);
    }
    
    @GetMapping("/service-breakdown")
    public ApiResponse<Map<String, Object>> getServiceBreakdown() {
        var report = analyticsService.generateReport("monthly");
        
        List<Map<String, Object>> services = new ArrayList<>();
        if (report != null && report.topSpenders() != null) {
            for (var spender : report.topSpenders()) {
                Map<String, Object> service = new HashMap<>();
                service.put("provider", "AWS");
                service.put("service", spender.getService());
                service.put("cost", spender.getCost());
                service.put("percentage", spender.getPercentage());
                service.put("category", "Compute");
                services.add(service);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("services", services);
        response.put("totalCost", report != null ? report.currentCost() : 0.0);
        response.put("servicesCount", services.size());
        
        return ApiResponse.success(response);
    }
}
