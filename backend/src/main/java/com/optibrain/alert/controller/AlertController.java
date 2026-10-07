package com.optibrain.alert.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getAllAlerts() {
        try {
            List<Map<String, Object>> alerts = alertService.getAllAlerts();
            return ApiResponse.success(alerts, "Alerts retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting alerts: {}", e.getMessage());
            return ApiResponse.error("Failed to get alerts: " + e.getMessage());
        }
    }

    @GetMapping("/severity/{severity}")
    public ApiResponse<List<Map<String, Object>>> getAlertsBySeverity(@PathVariable String severity) {
        try {
            List<Map<String, Object>> alerts = alertService.getAlertsBySeverity(severity);
            return ApiResponse.success(alerts, "Alerts retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting alerts by severity {}: {}", severity, e.getMessage());
            return ApiResponse.error("Failed to get alerts: " + e.getMessage());
        }
    }
    
    @PatchMapping("/{id}/acknowledge")
    public ApiResponse<Map<String, Object>> acknowledgeAlert(@PathVariable String id) {
        try {
            Map<String, Object> result = alertService.acknowledgeAlert(id);
            return ApiResponse.success(result, "Alert acknowledged successfully");
        } catch (Exception e) {
            log.error("Error acknowledging alert {}: {}", id, e.getMessage());
            return ApiResponse.error("Failed to acknowledge alert: " + e.getMessage());
        }
    }
}
