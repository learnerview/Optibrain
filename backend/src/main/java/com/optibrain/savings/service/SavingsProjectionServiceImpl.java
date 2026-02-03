package com.optibrain.savings.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of SavingsProjectionService
 */
@Service
public class SavingsProjectionServiceImpl implements SavingsProjectionService {
    
    @Override
    public Map<String, Object> getSavingsProjection() {
        Map<String, Object> projection = new HashMap<>();
        
        projection.put("currentMonthlyCost", 8920.30);
        projection.put("projectedMonthlyCost", 6580.55);
        projection.put("monthlySavings", 2339.75);
        projection.put("annualSavings", 28077.00);
        projection.put("savingsPercentage", 26.2);
        
        List<Map<String, Object>> monthlyBreakdown = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        double[] currentCosts = {8920.30, 9100.50, 8750.20, 8890.00, 9050.75, 8920.30};
        double[] projectedCosts = {6580.55, 6720.30, 6450.80, 6550.20, 6670.50, 6580.55};
        
        for (int i = 0; i < months.length; i++) {
            Map<String, Object> month = new HashMap<>();
            month.put("month", months[i]);
            month.put("currentCost", currentCosts[i]);
            month.put("projectedCost", projectedCosts[i]);
            month.put("savings", currentCosts[i] - projectedCosts[i]);
            monthlyBreakdown.add(month);
        }
        
        projection.put("monthlyBreakdown", monthlyBreakdown);
        
        return projection;
    }
}
