package com.optibrain.optimization.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Optimization History functionality
 */
public interface OptimizationHistoryService {
    
    /**
     * Get all optimization actions
     * @return List of optimization history items
     */
    List<Map<String, Object>> getAllOptimizations();
    
    /**
     * Get optimization by ID
     * @param id Optimization ID
     * @return Optimization details
     */
    Map<String, Object> getOptimizationById(String id);
    
    /**
     * Get optimizations by status
     * @param status Status filter (Applied, Pending, Simulated, Rejected)
     * @return List of optimizations
     */
    List<Map<String, Object>> getOptimizationsByStatus(String status);
}
