package com.optibrain.optimization.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Demo implementation of OptimizationHistoryService
 * Returns deterministic optimization history aligned with recommendations story
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.demo-mode", havingValue = "true")
public class DemoOptimizationHistoryService implements OptimizationHistoryService {

    private final List<Map<String, Object>> optimizations;

    public DemoOptimizationHistoryService() {
        optimizations = new ArrayList<>();
        
        // Optimization 1: Terminate Idle EC2 (from recommendations)
        optimizations.add(createOptimization(
            "opt-001",
            "Terminate Idle EC2 Instance",
            "i-023ab",
            "EC2 Instance i-023ab",
            "Simulated",
            8400,
            "High",
            "2026-01-15T10:30:00",
            null,
            "EC2 instance idle for 92% of the time. Terminating will save ₹8,400/month."
        ));
        
        // Optimization 2: Reduce RDS Storage (from recommendations)
        optimizations.add(createOptimization(
            "opt-002",
            "Reduce RDS Storage by 30%",
            "db-prod",
            "RDS MySQL db-prod",
            "Pending",
            4600,
            "High",
            "2026-01-15T10:32:00",
            null,
            "RDS storage over-provisioned. Reducing by 30% will save ₹4,600/month."
        ));
        
        // Optimization 3: Previous successful optimization
        optimizations.add(createOptimization(
            "opt-003",
            "Migrate to Spot Instances",
            "i-089gh",
            "EC2 Instance i-089gh",
            "Applied",
            6200,
            "High",
            "2026-01-10T14:20:00",
            "2026-01-12T09:15:00",
            "Successfully migrated to Spot Instance, saving ₹6,200/month."
        ));
        
        // Optimization 4: Rejected optimization
        optimizations.add(createOptimization(
            "opt-004",
            "Downgrade RDS Instance Type",
            "db-analytics",
            "RDS PostgreSQL db-analytics",
            "Rejected",
            3800,
            "Medium",
            "2026-01-08T11:45:00",
            null,
            "Rejected due to performance requirements."
        ));
    }

    @Override
    public List<Map<String, Object>> getAllOptimizations() {
        log.info("[DEMO] Getting all optimizations");
        return new ArrayList<>(optimizations);
    }

    @Override
    public Map<String, Object> getOptimizationById(String id) {
        log.info("[DEMO] Getting optimization by ID: {}", id);
        return optimizations.stream()
                .filter(o -> id.equals(o.get("id")))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Map<String, Object>> getOptimizationsByStatus(String status) {
        log.info("[DEMO] Getting optimizations by status: {}", status);
        return optimizations.stream()
                .filter(o -> status.equalsIgnoreCase((String) o.get("status")))
                .collect(Collectors.toList());
    }

    private Map<String, Object> createOptimization(
            String id,
            String action,
            String resourceId,
            String resourceName,
            String status,
            double monthlySavings,
            String confidence,
            String createdAt,
            String appliedAt,
            String description) {
        
        Map<String, Object> optimization = new HashMap<>();
        optimization.put("id", id);
        optimization.put("action", action);
        optimization.put("resourceId", resourceId);
        optimization.put("resourceName", resourceName);
        optimization.put("status", status);
        optimization.put("monthlySavings", monthlySavings);
        optimization.put("confidence", confidence);
        optimization.put("createdAt", createdAt);
        optimization.put("appliedAt", appliedAt);
        optimization.put("description", description);
        
        return optimization;
    }
}
