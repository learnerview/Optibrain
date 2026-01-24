package com.optibrain.costs.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.costs.service.CostExplorerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/costs")
@RequiredArgsConstructor
@Slf4j
public class CostExplorerController {

    private final CostExplorerService costExplorerService;

    @GetMapping("/explorer")
    public ApiResponse<Map<String, Object>> getCostExplorerData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String region) {
        
        try {
            // Default to last 30 days if not specified
            if (from == null) from = LocalDate.now().minusDays(30);
            if (to == null) to = LocalDate.now();
            
            Map<String, Object> data = costExplorerService.getCostData(from, to, service, region);
            return ApiResponse.success(data, "Cost explorer data retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting cost explorer data: {}", e.getMessage());
            return ApiResponse.error("Failed to get cost explorer data: " + e.getMessage());
        }
    }

    @GetMapping("/daily")
    public ApiResponse<List<Map<String, Object>>> getDailyCosts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        try {
            if (from == null) from = LocalDate.now().minusDays(30);
            if (to == null) to = LocalDate.now();
            
            List<Map<String, Object>> data = costExplorerService.getDailyCosts(from, to);
            return ApiResponse.success(data, "Daily costs retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting daily costs: {}", e.getMessage());
            return ApiResponse.error("Failed to get daily costs: " + e.getMessage());
        }
    }

    @GetMapping("/breakdown")
    public ApiResponse<List<Map<String, Object>>> getServiceBreakdown() {
        try {
            List<Map<String, Object>> data = costExplorerService.getServiceBreakdown();
            return ApiResponse.success(data, "Service breakdown retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting service breakdown: {}", e.getMessage());
            return ApiResponse.error("Failed to get service breakdown: " + e.getMessage());
        }
    }
}
