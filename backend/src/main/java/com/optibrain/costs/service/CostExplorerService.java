package com.optibrain.costs.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for Cost Explorer functionality
 */
public interface CostExplorerService {
    
    /**
     * Get cost data with filters
     * @param from Start date
     * @param to End date
     * @param service Service filter (EC2, RDS, S3, etc.)
     * @param region Region filter
     * @return Cost explorer data
     */
    Map<String, Object> getCostData(LocalDate from, LocalDate to, String service, String region);
    
    /**
     * Get daily cost breakdown
     * @param from Start date
     * @param to End date
     * @return List of daily costs
     */
    List<Map<String, Object>> getDailyCosts(LocalDate from, LocalDate to);
    
    /**
     * Get service breakdown
     * @return List of services with costs
     */
    List<Map<String, Object>> getServiceBreakdown();
}
