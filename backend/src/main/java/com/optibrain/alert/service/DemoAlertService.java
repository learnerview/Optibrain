package com.optibrain.alert.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Demo implementation of AlertService
 */
@Service
@Slf4j
public class DemoAlertService implements AlertService {

    private final List<Map<String, Object>> alerts;

    public DemoAlertService() {
        alerts = new ArrayList<>();
        
        // Alert 1: Cost spike (aligned with anomaly story)
        alerts.add(createAlert(
            "alert-001",
            "COST_SPIKE",
            "HIGH",
            "EC2 costs increased by 116% on 2026-01-14",
            "EC2",
            "2026-01-14T08:15:00",
            false
        ));
        
        // Alert 2: Idle resource
        alerts.add(createAlert(
            "alert-002",
            "IDLE_RESOURCE",
            "MEDIUM",
            "EC2 Instance i-023ab idle for 92% of the time",
            "EC2",
            "2026-01-15T09:00:00",
            false
        ));
        
        // Alert 3: Optimization available
        alerts.add(createAlert(
            "alert-003",
            "OPTIMIZATION_AVAILABLE",
            "LOW",
            "2 new optimization recommendations available",
            "Multiple",
            "2026-01-15T10:30:00",
            false
        ));
    }

    @Override
    public List<Map<String, Object>> getAllAlerts() {
        log.info("[DEMO] Getting all alerts");
        return new ArrayList<>(alerts);
    }

    @Override
    public List<Map<String, Object>> getAlertsBySeverity(String severity) {
        log.info("[DEMO] Getting alerts by severity: {}", severity);
        return alerts.stream()
                .filter(a -> severity.equalsIgnoreCase((String) a.get("severity")))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> acknowledgeAlert(String id) {
        log.info("[DEMO] Acknowledging alert: {}", id);
        
        for (Map<String, Object> alert : alerts) {
            if (id.equals(alert.get("id"))) {
                alert.put("acknowledged", true);
                alert.put("acknowledgedAt", LocalDateTime.now().toString());
                log.info("[DEMO] Alert {} acknowledged successfully", id);
                return alert;
            }
        }
        
        log.warn("[DEMO] Alert {} not found", id);
        throw new RuntimeException("Alert not found: " + id);
    }

    private Map<String, Object> createAlert(
            String id,
            String type,
            String severity,
            String message,
            String service,
            String timestamp,
            boolean acknowledged) {
        
        Map<String, Object> alert = new HashMap<>();
        alert.put("id", id);
        alert.put("type", type);
        alert.put("severity", severity);
        alert.put("message", message);
        alert.put("service", service);
        alert.put("timestamp", timestamp);
        alert.put("acknowledged", acknowledged);
        
        return alert;
    }
}
