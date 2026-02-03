package com.optibrain.costs.controller;

import com.optibrain.costs.service.CostReportService;
import com.optibrain.costs.service.IdleResourceDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for AWS cost analysis and optimization
 */
@RestController
@RequestMapping("/api/costs")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class CostAnalysisController {

    private final CostReportService costReportService;
    private final IdleResourceDetectionService idleResourceDetectionService;

    /**
     * Generate a daily cost report for the past N days
     */
    @GetMapping("/report/daily")
    public ResponseEntity<CostReportService.CostReport> getDailyCostReport(
            @RequestParam(defaultValue = "7") int days) {
        CostReportService.CostReport report = costReportService.generateDailyCostReport(days);
        return ResponseEntity.ok(report);
    }

    /**
     * Generate a weekly cost report for the past N weeks
     */
    @GetMapping("/report/weekly")
    public ResponseEntity<CostReportService.CostReport> getWeeklyCostReport(
            @RequestParam(defaultValue = "4") int weeks) {
        CostReportService.CostReport report = costReportService.generateWeeklyCostReport(weeks);
        return ResponseEntity.ok(report);
    }

    /**
     * Get cost forecast for the next N days
     */
    @GetMapping("/forecast")
    public ResponseEntity<CostReportService.CostForecast> getCostForecast(
            @RequestParam(defaultValue = "30") int days) {
        CostReportService.CostForecast forecast = costReportService.getCostForecast(days);
        return ResponseEntity.ok(forecast);
    }

    /**
     * Get cost breakdown by service
     */
    @GetMapping("/breakdown")
    public ResponseEntity<Map<String, Double>> getCostByService(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(30);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();
        
        Map<String, Double> breakdown = costReportService.getCostByService(start, end);
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Detect idle EC2 instances
     */
    @GetMapping("/idle-instances")
    public ResponseEntity<Map<String, IdleResourceDetectionService.IdleInstanceInfo>> getIdleInstances() {
        Map<String, IdleResourceDetectionService.IdleInstanceInfo> idleInstances = 
                idleResourceDetectionService.detectIdleEC2Instances();
        return ResponseEntity.ok(idleInstances);
    }

    /**
     * Detect stopped EC2 instances
     */
    @GetMapping("/stopped-instances")
    public ResponseEntity<List<IdleResourceDetectionService.StoppedInstanceInfo>> getStoppedInstances() {
        List<IdleResourceDetectionService.StoppedInstanceInfo> stoppedInstances = 
                idleResourceDetectionService.detectStoppedInstances();
        return ResponseEntity.ok(stoppedInstances);
    }
}
