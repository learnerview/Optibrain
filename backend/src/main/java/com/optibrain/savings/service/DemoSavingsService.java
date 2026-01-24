package com.optibrain.savings.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoSavingsService implements SavingsProjectionService {

    @Override
    public Map<String, Object> getSavingsProjection() {
        return Map.of(
            "monthlySavings", 13000,
            "annualSavings", 156000,
            "confidence", "94%",
            "assumptions", List.of(
                "Idle resources removed",
                "No increase in traffic",
                "Stable workload pattern"
            )
        );
    }
}
