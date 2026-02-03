package com.optibrain.alert.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of AlertService
 */
@Service
public class AlertServiceImpl implements AlertService {
    
    private List<Map<String, Object>> alerts = new ArrayList<>();
    
    public AlertServiceImpl() {
        // Initialize with some sample alerts for demo
        initializeSampleAlerts();
    }
    
    private void initializeSampleAlerts() {
        alerts.add(createAlert("1", "HIGH", "Cost spike detected", "EC2 costs increased by 45% in the last 24 hours", false));
        alerts.add(createAlert("2", "MEDIUM", "Idle resources found", "3 EC2 instances with <5% CPU utilization detected", false));
        alerts.add(createAlert("3", "LOW", "Recommendation available", "Switch to Reserved Instances for 20% savings", true));
    }
    
    private Map<String, Object> createAlert(String id, String severity, String title, String message, boolean acknowledged) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("id", id);
        alert.put("severity", severity);
        alert.put("title", title);
        alert.put("message", message);
        alert.put("acknowledged", acknowledged);
        alert.put("timestamp", Instant.now().toString());
        return alert;
    }
    
    @Override
    public List<Map<String, Object>> getAllAlerts() {
        return new ArrayList<>(alerts);
    }
    
    @Override
    public List<Map<String, Object>> getAlertsBySeverity(String severity) {
        return alerts.stream()
                .filter(alert -> severity.equalsIgnoreCase((String) alert.get("severity")))
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> acknowledgeAlert(String id) {
        for (Map<String, Object> alert : alerts) {
            if (id.equals(alert.get("id"))) {
                alert.put("acknowledged", true);
                return alert;
            }
        }
        return null;
    }
}
