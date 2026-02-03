package com.optibrain.report.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ReportService
 */
@Service
public class ReportServiceImpl implements ReportService {
    
    @Override
    public Map<String, Object> getMonthlyReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("reportMonth", LocalDate.now().getMonth().toString());
        report.put("reportYear", LocalDate.now().getYear());
        report.put("generatedDate", LocalDate.now().toString());
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCost", 8920.30);
        summary.put("previousMonthCost", 8450.50);
        summary.put("costChange", 469.80);
        summary.put("costChangePercentage", 5.56);
        summary.put("totalResources", 145);
        summary.put("optimizationsApplied", 8);
        summary.put("savingsRealized", 1200.50);
        report.put("summary", summary);
        
        List<Map<String, Object>> topCostServices = new ArrayList<>();
        topCostServices.add(createServiceSummary("EC2", 4520.30, 12));
        topCostServices.add(createServiceSummary("RDS", 2840.50, 5));
        topCostServices.add(createServiceSummary("S3", 1650.75, 25));
        report.put("topCostServices", topCostServices);
        
        List<Map<String, Object>> recommendations = new ArrayList<>();
        recommendations.add(createRecommendation("Resize underutilized EC2 instances", 450.50));
        recommendations.add(createRecommendation("Delete unused EBS volumes", 120.30));
        recommendations.add(createRecommendation("Switch to Reserved Instances", 1200.00));
        report.put("topRecommendations", recommendations);
        
        return report;
    }
    
    private Map<String, Object> createServiceSummary(String service, double cost, int count) {
        Map<String, Object> serviceSummary = new HashMap<>();
        serviceSummary.put("service", service);
        serviceSummary.put("cost", cost);
        serviceSummary.put("resourceCount", count);
        return serviceSummary;
    }
    
    private Map<String, Object> createRecommendation(String description, double potentialSavings) {
        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("description", description);
        recommendation.put("potentialSavings", potentialSavings);
        return recommendation;
    }
}
