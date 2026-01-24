package com.optibrain.analytics.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoAnomalyService implements AnomalyService {

    @Override
    public Map<String, Object> getAnomalies() {
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
}
