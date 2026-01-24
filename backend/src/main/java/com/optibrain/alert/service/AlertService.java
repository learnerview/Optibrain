package com.optibrain.alert.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Alerts functionality
 */
public interface AlertService {
    
    /**
     * Get all alerts
     * @return List of alerts
     */
    List<Map<String, Object>> getAllAlerts();
    
    /**
     * Get alerts by severity
     * @param severity Severity level (HIGH, MEDIUM, LOW)
     * @return List of alerts
     */
    List<Map<String, Object>> getAlertsBySeverity(String severity);
    
    /**
     * Acknowledge an alert
     * @param id Alert ID
     * @return Updated alert
     */
    Map<String, Object> acknowledgeAlert(String id);
}
