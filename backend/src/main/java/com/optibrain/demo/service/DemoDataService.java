package com.optibrain.demo.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class DemoDataService {

    public Map<String, Object> getDashboardOverview() {
        return Map.of(
            "totalMonthlyCost", 128450,
            "currency", "INR",
            "trend", List.of(
                Map.of("month", "Aug", "cost", 94500),
                Map.of("month", "Sep", "cost", 101200),
                Map.of("month", "Oct", "cost", 110300),
                Map.of("month", "Nov", "cost", 118900),
                Map.of("month", "Dec", "cost", 125600),
                Map.of("month", "Jan", "cost", 128450)
            ),
            "topServices", List.of(
                Map.of("name", "EC2", "cost", 62400),
                Map.of("name", "RDS", "cost", 31400),
                Map.of("name", "S3", "cost", 21200),
                Map.of("name", "CloudWatch", "cost", 13450)
            )
        );
    }

    public Map<String, Object> getAnomaly() {
        return Map.of(
            "anomalyDetected", true,
            "severity", "HIGH",
            "date", "2026-01-14",
            "service", "EC2",
            "expectedCost", 4200,
            "actualCost", 9100,
            "reason", "Sudden increase in on-demand EC2 usage"
        );
    }

    public List<Map<String, Object>> getRecommendations() {
        return List.of(
            Map.of(
                "resource", "EC2 Instance i-023ab",
                "issue", "Idle for 92% of the time",
                "recommendation", "Downscale or stop instance",
                "monthlySavings", 8400
            ),
            Map.of(
                "resource", "RDS MySQL db-prod",
                "issue", "Over-provisioned storage",
                "recommendation", "Reduce allocated storage by 30%",
                "monthlySavings", 4600
            )
        );
    }

    public Map<String, Object> getSavingsProjection() {
        return Map.of(
            "monthlySavings", 13000,
            "annualSavings", 156000,
            "confidence", "High",
            "assumptions", List.of(
                "Idle resources removed",
                "No increase in traffic",
                "Stable workload pattern"
            )
        );
    }
}
