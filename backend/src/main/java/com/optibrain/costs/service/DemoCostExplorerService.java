package com.optibrain.costs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Demo implementation of CostExplorerService
 * Returns deterministic data aligned with the demo story
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoCostExplorerService implements CostExplorerService {

    @Override
    public Map<String, Object> getCostData(LocalDate from, LocalDate to, String service, String region) {
        log.info("[DEMO] Getting cost explorer data: from={}, to={}, service={}, region={}", 
                from, to, service, region);
        
        Map<String, Object> data = new HashMap<>();
        
        // Filters
        Map<String, String> filters = new HashMap<>();
        filters.put("service", service != null ? service : "All");
        filters.put("region", region != null ? region : "ap-south-1");
        filters.put("dateRange", "30d");
        data.put("filters", filters);
        
        // Daily costs - showing EC2 spike on Jan 14
        data.put("dailyCosts", getDailyCosts(from, to));
        
        // Service breakdown
        data.put("breakdown", getServiceBreakdown());
        
        return data;
    }

    @Override
    public List<Map<String, Object>> getDailyCosts(LocalDate from, LocalDate to) {
        List<Map<String, Object>> dailyCosts = new ArrayList<>();
        
        // Demo data showing spike on Jan 14 (aligned with anomaly story)
        dailyCosts.add(createDailyCost("2026-01-10", 2100));
        dailyCosts.add(createDailyCost("2026-01-11", 2150));
        dailyCosts.add(createDailyCost("2026-01-12", 2080));
        dailyCosts.add(createDailyCost("2026-01-13", 2200));
        dailyCosts.add(createDailyCost("2026-01-14", 9100)); // SPIKE - matches anomaly
        dailyCosts.add(createDailyCost("2026-01-15", 2300));
        dailyCosts.add(createDailyCost("2026-01-16", 2250));
        dailyCosts.add(createDailyCost("2026-01-17", 2180));
        dailyCosts.add(createDailyCost("2026-01-18", 2220));
        dailyCosts.add(createDailyCost("2026-01-19", 2190));
        dailyCosts.add(createDailyCost("2026-01-20", 2210));
        
        return dailyCosts;
    }

    @Override
    public List<Map<String, Object>> getServiceBreakdown() {
        List<Map<String, Object>> breakdown = new ArrayList<>();
        
        // Aligned with dashboard overview story
        breakdown.add(createServiceCost("EC2", 62400));
        breakdown.add(createServiceCost("RDS", 31400));
        breakdown.add(createServiceCost("S3", 21200));
        breakdown.add(createServiceCost("CloudWatch", 13450));
        
        return breakdown;
    }
    
    private Map<String, Object> createDailyCost(String date, double cost) {
        Map<String, Object> daily = new HashMap<>();
        daily.put("date", date);
        daily.put("cost", cost);
        return daily;
    }
    
    private Map<String, Object> createServiceCost(String service, double cost) {
        Map<String, Object> serviceCost = new HashMap<>();
        serviceCost.put("service", service);
        serviceCost.put("cost", cost);
        return serviceCost;
    }
}
