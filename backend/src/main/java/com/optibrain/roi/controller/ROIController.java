package com.optibrain.roi.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.roi.service.ROITrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roi")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class ROIController {
    
    private final ROITrackingService roiTrackingService;
    
    @GetMapping("/executive-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'FINOPS_ANALYST')")
    public ApiResponse<Map<String, Object>> getExecutiveSummary() {
        try {
            Map<String, Object> summary = roiTrackingService.getExecutiveSummary();
            return ApiResponse.success(summary, "Executive summary retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to generate executive summary: {}", e.getMessage());
            return ApiResponse.error("Failed to generate executive summary: " + e.getMessage());
        }
    }
    
    @GetMapping("/real-time-metrics")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> getRealTimeMetrics() {
        try {
            Map<String, Object> metrics = roiTrackingService.getRealTimeMetrics();
            return ApiResponse.success(metrics, "Real-time metrics retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to retrieve real-time metrics: {}", e.getMessage());
            return ApiResponse.error("Failed to retrieve real-time metrics: " + e.getMessage());
        }
    }
    
    @GetMapping("/top-performers")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'FINOPS_ANALYST')")
    public ApiResponse<List<Map<String, Object>>> getTopPerformers() {
        try {
            List<Map<String, Object>> performers = roiTrackingService.getTopPerformers();
            return ApiResponse.success(performers, "Top performers retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to retrieve top performers: {}", e.getMessage());
            return ApiResponse.error("Failed to retrieve top performers: " + e.getMessage());
        }
    }
    
    @GetMapping("/tenant/{tenantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> getTenantROI(@PathVariable String tenantId) {
        try {
            var report = roiTrackingService.generateROIReport(tenantId);
            Map<String, Object> result = Map.of(
                "tenantId", report.getTenantId(),
                "tenantName", report.getTenantName(),
                "roiMetrics", report.getRoiMetrics(),
                "savingsReport", report.getSavingsReport(),
                "totalRecommendations", report.getTotalRecommendations(),
                "executedRecommendations", report.getExecutedRecommendations(),
                "executionRate", report.getTotalRecommendations() > 0 ? 
                    (double) report.getExecutedRecommendations() / report.getTotalRecommendations() * 100 : 0,
                "autonomousActionsCount", report.getAutonomousActionsCount(),
                "reportGeneratedAt", report.getReportGeneratedAt()
            );
            return ApiResponse.success(result, "Tenant ROI report retrieved successfully");
        } catch (Exception e) {
            log.error("Failed to generate ROI report for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to generate ROI report: " + e.getMessage());
        }
    }
}
