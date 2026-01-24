package com.optibrain.optimization.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.optimization.service.OptimizationHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/optimizations")
@RequiredArgsConstructor
@Slf4j
public class OptimizationHistoryController {

    private final OptimizationHistoryService optimizationHistoryService;

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getOptimizationHistory() {
        try {
            List<Map<String, Object>> history = optimizationHistoryService.getAllOptimizations();
            return ApiResponse.success(history, "Optimization history retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting optimization history: {}", e.getMessage());
            return ApiResponse.error("Failed to get optimization history: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getOptimizationById(@PathVariable String id) {
        try {
            Map<String, Object> optimization = optimizationHistoryService.getOptimizationById(id);
            if (optimization == null) {
                return ApiResponse.error("Optimization not found");
            }
            return ApiResponse.success(optimization, "Optimization retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting optimization {}: {}", id, e.getMessage());
            return ApiResponse.error("Failed to get optimization: " + e.getMessage());
        }
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<Map<String, Object>>> getOptimizationsByStatus(@PathVariable String status) {
        try {
            List<Map<String, Object>> optimizations = optimizationHistoryService.getOptimizationsByStatus(status);
            return ApiResponse.success(optimizations, "Optimizations retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting optimizations by status {}: {}", status, e.getMessage());
            return ApiResponse.error("Failed to get optimizations: " + e.getMessage());
        }
    }
}
