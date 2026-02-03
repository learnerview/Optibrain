package com.optibrain.optimization.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of OptimizationHistoryService
 */
@Service
public class OptimizationHistoryServiceImpl implements OptimizationHistoryService {
    
    private List<Map<String, Object>> optimizations = new ArrayList<>();
    
    public OptimizationHistoryServiceImpl() {
        initializeSampleData();
    }
    
    private void initializeSampleData() {
        optimizations.add(createOptimization("1", "Resize EC2 instance i-1234", "Applied", 150.50, "EC2"));
        optimizations.add(createOptimization("2", "Delete unused EBS volume", "Pending", 85.20, "EBS"));
        optimizations.add(createOptimization("3", "Switch to Reserved Instance", "Simulated", 420.00, "EC2"));
        optimizations.add(createOptimization("4", "Optimize S3 storage class", "Applied", 75.30, "S3"));
        optimizations.add(createOptimization("5", "Remove idle RDS instance", "Rejected", 280.00, "RDS"));
    }
    
    private Map<String, Object> createOptimization(String id, String action, String status, double savings, String service) {
        Map<String, Object> optimization = new HashMap<>();
        optimization.put("id", id);
        optimization.put("action", action);
        optimization.put("status", status);
        optimization.put("estimatedSavings", savings);
        optimization.put("service", service);
        optimization.put("timestamp", Instant.now().toString());
        return optimization;
    }
    
    @Override
    public List<Map<String, Object>> getAllOptimizations() {
        return new ArrayList<>(optimizations);
    }
    
    @Override
    public Map<String, Object> getOptimizationById(String id) {
        return optimizations.stream()
                .filter(opt -> id.equals(opt.get("id")))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public List<Map<String, Object>> getOptimizationsByStatus(String status) {
        return optimizations.stream()
                .filter(opt -> status.equalsIgnoreCase((String) opt.get("status")))
                .collect(Collectors.toList());
    }
}
