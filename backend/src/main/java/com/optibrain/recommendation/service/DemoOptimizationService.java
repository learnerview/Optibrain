package com.optibrain.recommendation.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoOptimizationService implements OptimizationService {

    @Override
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
}
