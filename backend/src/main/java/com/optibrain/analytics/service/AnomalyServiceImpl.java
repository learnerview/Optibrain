package com.optibrain.analytics.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Implementation of AnomalyService
 */
@Service
public class AnomalyServiceImpl implements AnomalyService {
    
    @Override
    public Map<String, Object> getAnomalies() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> anomalies = new ArrayList<>();
        
        // Sample anomalies
        anomalies.add(createAnomaly("Cost spike in EC2", "45% increase detected", "HIGH", Instant.now()));
        anomalies.add(createAnomaly("Unusual API calls", "250% increase in S3 API calls", "MEDIUM", Instant.now().minusSeconds(3600)));
        
        response.put("anomalies", anomalies);
        response.put("count", anomalies.size());
        response.put("timestamp", Instant.now().toString());
        
        return response;
    }
    
    private Map<String, Object> createAnomaly(String title, String description, String severity, Instant timestamp) {
        Map<String, Object> anomaly = new HashMap<>();
        anomaly.put("title", title);
        anomaly.put("description", description);
        anomaly.put("severity", severity);
        anomaly.put("timestamp", timestamp.toString());
        anomaly.put("status", "ACTIVE");
        return anomaly;
    }
}
