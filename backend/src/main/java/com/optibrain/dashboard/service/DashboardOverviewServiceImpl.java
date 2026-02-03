package com.optibrain.dashboard.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of DashboardOverviewService
 */
@Service
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DashboardOverviewServiceImpl implements DashboardOverviewService {
    
    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        overview.put("totalCost", 12450.50);
        overview.put("monthlyCost", 8920.30);
        overview.put("projectedSavings", 2340.75);
        overview.put("activeResources", 145);
        overview.put("idleResources", 12);
        overview.put("optimizationScore", 78);
        overview.put("alertCount", 5);
        overview.put("currency", "USD");
        
        return overview;
    }
}
