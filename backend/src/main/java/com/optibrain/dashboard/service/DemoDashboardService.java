package com.optibrain.dashboard.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoDashboardService implements DashboardOverviewService {

    @Override
    public Map<String, Object> getOverview() {
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
            ),
            "lastUpdated", LocalDateTime.now()
        );
    }
}
