package com.optibrain.policy.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {
    private String id;
    private String name;
    private String description;
    private String type;
    private Map<String, Object> rules;
    private boolean enabled;
    private int priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    public Policy(String id, String name, String type, boolean enabled) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.enabled = enabled;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.rules = new java.util.HashMap<>();
    }
    
    // Getter methods for DecisionScorer
    public double getPerformanceWeight() {
        return ((Number) rules.getOrDefault("performanceWeight", 0.5)).doubleValue();
    }
    
    public double getCostWeight() {
        return ((Number) rules.getOrDefault("costWeight", 0.3)).doubleValue();
    }
    
    public double getCpuThreshold() {
        return ((Number) rules.getOrDefault("cpuThreshold", 80.0)).doubleValue();
    }
    
    public boolean isAutoOptimizationEnabled() {
        return (Boolean) rules.getOrDefault("autoOptimizationEnabled", true);
    }
    
    public String getRegion() {
        return (String) rules.getOrDefault("region", "us-east-1");
    }
    
    @SuppressWarnings("unchecked")
    public java.util.List<String> getDeniedResources() {
        Object denied = rules.get("deniedResources");
        if (denied instanceof java.util.List) {
            return (java.util.List<String>) denied;
        }
        return java.util.Collections.emptyList();
    }
    
    @SuppressWarnings("unchecked")
    public java.util.List<String> getAllowedResources() {
        Object allowed = rules.get("allowedResources");
        if (allowed instanceof java.util.List) {
            return (java.util.List<String>) allowed;
        }
        return java.util.Collections.emptyList();
    }
    
    @SuppressWarnings("unchecked")
    public java.util.List<String> getRestrictedActions() {
        Object restricted = rules.get("restrictedActions");
        if (restricted instanceof java.util.List) {
            return (java.util.List<String>) restricted;
        }
        return java.util.Collections.emptyList();
    }
}
