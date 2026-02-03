package com.optibrain.costs.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of CostExplorerService
 */
@Service
public class CostExplorerServiceImpl implements CostExplorerService {
    
    @Override
    public Map<String, Object> getCostData(LocalDate from, LocalDate to, String service, String region) {
        Map<String, Object> costData = new HashMap<>();
        
        costData.put("from", from != null ? from.toString() : LocalDate.now().minusDays(30).toString());
        costData.put("to", to != null ? to.toString() : LocalDate.now().toString());
        costData.put("service", service != null ? service : "ALL");
        costData.put("region", region != null ? region : "ALL");
        costData.put("totalCost", 12450.50);
        costData.put("currency", "USD");
        
        List<Map<String, Object>> dailyCosts = getDailyCosts(from, to);
        costData.put("dailyCosts", dailyCosts);
        
        List<Map<String, Object>> serviceBreakdown = getServiceBreakdown();
        costData.put("serviceBreakdown", serviceBreakdown);
        
        return costData;
    }
    
    @Override
    public List<Map<String, Object>> getDailyCosts(LocalDate from, LocalDate to) {
        List<Map<String, Object>> dailyCosts = new ArrayList<>();
        
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(30);
        LocalDate endDate = to != null ? to : LocalDate.now();
        
        double baseCost = 300.0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            Map<String, Object> dayCost = new HashMap<>();
            dayCost.put("date", current.toString());
            dayCost.put("cost", baseCost + (Math.random() * 100));
            dailyCosts.add(dayCost);
            current = current.plusDays(1);
        }
        
        return dailyCosts;
    }
    
    @Override
    public List<Map<String, Object>> getServiceBreakdown() {
        List<Map<String, Object>> services = new ArrayList<>();
        
        services.add(createServiceCost("EC2", 4520.30, 36.3));
        services.add(createServiceCost("RDS", 2840.50, 22.8));
        services.add(createServiceCost("S3", 1650.75, 13.3));
        services.add(createServiceCost("Lambda", 980.20, 7.9));
        services.add(createServiceCost("CloudFront", 720.40, 5.8));
        services.add(createServiceCost("Other", 1738.35, 13.9));
        
        return services;
    }
    
    private Map<String, Object> createServiceCost(String serviceName, double cost, double percentage) {
        Map<String, Object> service = new HashMap<>();
        service.put("service", serviceName);
        service.put("cost", cost);
        service.put("percentage", percentage);
        return service;
    }
}
