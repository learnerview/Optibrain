package com.optibrain.dashboard.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.config.DynamicConfig;
import com.optibrain.decision.service.DecisionEngineService;
import com.optibrain.decision.model.DecisionRequest;
import com.optibrain.decision.model.DecisionResponse;
import com.optibrain.decision.model.DecisionType;
import com.optibrain.metrics.provider.TenantAwareMetricsProvider;
import com.optibrain.metrics.model.MetricData;
import com.optibrain.roi.model.TenantROIReport;
import com.optibrain.roi.service.ROITrackingService;
import com.optibrain.security.service.CompanyManagementService;
import com.optibrain.tenant.service.TenantCredentialService;
import com.optibrain.chat.service.PyBridgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController("mainDashboardController")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class DashboardController {
    
    private final TenantAwareMetricsProvider metricsProvider;
    private final ROITrackingService roiService;
    private final TenantCredentialService tenantService;
    private final DecisionEngineService decisionEngine;
    private final CompanyManagementService companyService;
    private final PyBridgeService mlService;
    private final DynamicConfig config;
    
    @GetMapping("/overview")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> getDashboardOverview() {
        try {
            // Get executive summary
            Map<String, Object> executiveSummary = roiService.getExecutiveSummary();
            
            // Get real-time metrics
            Map<String, Object> realTimeMetrics = roiService.getRealTimeMetrics();
            
            // Get top performers
            List<Map<String, Object>> topPerformers = roiService.getTopPerformers();
            
            // Get recent decisions
            List<Map<String, Object>> recentDecisions = getRecentDecisions();
            
            // Get system health
            Map<String, Object> systemHealth = getSystemHealth();
            
            Map<String, Object> overview = Map.of(
                "executiveSummary", executiveSummary,
                "realTimeMetrics", realTimeMetrics,
                "topPerformers", topPerformers,
                "recentDecisions", recentDecisions,
                "systemHealth", systemHealth,
                "lastUpdated", LocalDateTime.now()
            );
            
            return ApiResponse.success(overview, "Dashboard overview retrieved successfully");
            
        } catch (Exception e) {
            log.error("Error getting dashboard overview: {}", e.getMessage());
            return ApiResponse.error("Failed to get dashboard overview: " + e.getMessage());
        }
    }
    
    @GetMapping("/metrics/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<List<MetricData>> getTenantMetrics(@PathVariable String tenantId) {
        try {
            List<MetricData> metrics = metricsProvider.fetchMetricsForTenant(tenantId);
            return ApiResponse.success(metrics, "Tenant metrics retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting tenant metrics: {}", e.getMessage());
            return ApiResponse.error("Failed to get tenant metrics: " + e.getMessage());
        }
    }
    
    @GetMapping("/predictions/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> getPredictions(@PathVariable String tenantId) {
        try {
            // Get ML predictions for the tenant
            Map<String, Object> predictions = Map.of(
                "cpuForecast", mlService.getPrediction(getHistoricalData(tenantId, "cpu")),
                "memoryForecast", mlService.getPrediction(getHistoricalData(tenantId, "memory")),
                "costForecast", mlService.getPrediction(getHistoricalData(tenantId, "cost")),
                "anomalies", mlService.getAnomalies(),
                "optimization", mlService.getCostOptimization(getCurrentMetrics(tenantId))
            );
            
            return ApiResponse.success(predictions, "Predictions retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting predictions: {}", e.getMessage());
            return ApiResponse.error("Failed to get predictions: " + e.getMessage());
        }
    }
    
    @PostMapping("/decisions")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST')")
    public ApiResponse<DecisionResponse> makeDecision(@RequestBody DecisionRequest request) {
        try {
            DecisionResponse response = decisionEngine.makeDecision(request);
            return ApiResponse.success(response, "Decision made successfully");
        } catch (Exception e) {
            log.error("Error making decision: {}", e.getMessage());
            return ApiResponse.error("Failed to make decision: " + e.getMessage());
        }
    }
    
    @PostMapping("/decisions/{decisionId}/execute")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST')")
    public ApiResponse<Map<String, Object>> executeDecision(
            @PathVariable String decisionId,
            @RequestParam String userId) {
        
        try {
            Map<String, Object> response = decisionEngine.executeDecision(decisionId, userId);
            return ApiResponse.success(response, "Decision executed successfully");
        } catch (Exception e) {
            log.error("Error executing decision: {}", e.getMessage());
            return ApiResponse.error("Failed to execute decision: " + e.getMessage());
        }
    }
    
    @GetMapping("/roi/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<TenantROIReport> getTenantROI(@PathVariable String tenantId) {
        try {
            TenantROIReport roiReport = roiService.generateROIReport(tenantId);
            return ApiResponse.success(roiReport, "ROI data retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting tenant ROI: {}", e.getMessage());
            return ApiResponse.error("Failed to get ROI data: " + e.getMessage());
        }
    }
    
    // Config endpoints moved to ConfigurationController (GET/PUT /api/config/{tenantId})

    
    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<Map<String, Object>>> getAlerts() {
        try {
            List<Map<String, Object>> alerts = List.of(
                Map.of(
                    "id", "1",
                    "type", "BUDGET_ALERT",
                    "severity", "HIGH",
                    "message", "Monthly budget exceeded by 15%",
                    "tenantId", "tenant-123",
                    "timestamp", LocalDateTime.now().minusHours(2),
                    "actionRequired", true
                ),
                Map.of(
                    "id", "2",
                    "type", "ANOMALY_DETECTED",
                    "severity", "MEDIUM",
                    "message", "Unusual CPU spike detected",
                    "tenantId", "tenant-456",
                    "timestamp", LocalDateTime.now().minusHours(4),
                    "actionRequired", false
                )
            );
            
            return ApiResponse.success(alerts, "Alerts retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting alerts: {}", e.getMessage());
            return ApiResponse.error("Failed to get alerts: " + e.getMessage());
        }
    }
    
    @GetMapping("/recommendations/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<List<Map<String, Object>>> getRecommendations(@PathVariable String tenantId) {
        try {
            List<Map<String, Object>> recommendations = List.of(
                Map.of(
                    "id", "rec-1",
                    "type", "RIGHTSIZE_DOWN",
                    "title", "Downsize EC2 instance",
                    "description", "CPU utilization is consistently below 20%",
                    "expectedSavings", 45.50,
                    "confidence", 0.85,
                    "riskLevel", "LOW",
                    "actionRequired", true,
                    "autoExecutable", true
                ),
                Map.of(
                    "id", "rec-2",
                    "type", "SCHEDULE_OPTIMIZATION",
                    "title", "Implement start/stop schedule",
                    "description", "Resources running 24/7 but usage only during business hours",
                    "expectedSavings", 120.75,
                    "confidence", 0.92,
                    "riskLevel", "LOW",
                    "actionRequired", true,
                    "autoExecutable", true
                )
            );
            
            return ApiResponse.success(recommendations, "Recommendations retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting recommendations: {}", e.getMessage());
            return ApiResponse.error("Failed to get recommendations: " + e.getMessage());
        }
    }
    
    // Helper methods
    private List<Map<String, Object>> getRecentDecisions() {
        return List.of(
            Map.of(
                "id", "dec-1",
                "type", "COST_OPTIMIZATION",
                "action", "RIGHTSIZE_DOWN",
                "status", "EXECUTED",
                "confidence", 0.87,
                "expectedSavings", 25.50,
                "actualSavings", 23.75,
                "timestamp", LocalDateTime.now().minusHours(1),
                "tenantId", "tenant-123"
            ),
            Map.of(
                "id", "dec-2",
                "type", "ANOMALY_DETECTION",
                "action", "INVESTIGATE",
                "status", "PENDING",
                "confidence", 0.92,
                "riskLevel", "MEDIUM",
                "timestamp", LocalDateTime.now().minusHours(3),
                "tenantId", "tenant-456"
            )
        );
    }
    
    private Map<String, Object> getSystemHealth() {
        return Map.of(
            "status", "HEALTHY",
            "mlService", "ONLINE",
            "database", "ONLINE",
            "cache", "ONLINE",
            "lastHealthCheck", LocalDateTime.now(),
            "uptime", "99.9%",
            "responseTime", "125ms"
        );
    }
    
    private List<Double> getHistoricalData(String tenantId, String metricType) {
        // In a real implementation, this would fetch from database
        return List.of(45.2, 47.1, 46.8, 48.3, 49.1, 47.5, 46.2);
    }
    
    private Map<String, Object> getCurrentMetrics(String tenantId) {
        // In a real implementation, this would fetch current metrics
        return Map.of(
            "cpu_utilization", 35.5,
            "memory_utilization", 67.2,
            "hourly_cost", 25.75,
            "instance_count", 4
        );
    }
}
