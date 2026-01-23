package com.optibrain.metrics.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.time.LocalDateTime;

@Service
public class AnomalyDetectorService {
    
    private final Random random = new Random();

    public List<Map<String, Object>> detectAnomalies() {
        List<Map<String, Object>> anomalies = new ArrayList<>();
        
        // Simulate checking metrics
        if (random.nextDouble() > 0.7) {
            anomalies.add(Map.of(
                "id", "ANOM-" + System.currentTimeMillis(),
                "resourceId", "i-0a8b9c7d6e5f4g3h2",
                "type", "CPU_SPIKE",
                "severity", "HIGH",
                "description", "CPU utilization > 95% for 15 mins",
                "timestamp", LocalDateTime.now().minusMinutes(15).toString(),
                "costImpact", 12.50
            ));
        }

        if (random.nextDouble() > 0.6) {
             anomalies.add(Map.of(
                "id", "ANOM-" + (System.currentTimeMillis() + 1),
                "resourceId", "db-prod-primary",
                "type", "COST_SURGE",
                "severity", "MEDIUM",
                "description", "IOPS cost increase by 40%",
                "timestamp", LocalDateTime.now().minusHours(2).toString(),
                "costImpact", 45.20
            ));
        }

        return anomalies;
    }
}
