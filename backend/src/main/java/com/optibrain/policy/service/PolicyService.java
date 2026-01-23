package com.optibrain.policy.service;

import com.optibrain.policy.model.Policy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PolicyService {
    
    private final Map<String, Policy> policyCache = new ConcurrentHashMap<>();
    
    public PolicyService() {
        initializeDefaultPolicies();
    }
    
    public Policy getPolicy(String id) {
        return policyCache.get(id);
    }
    
    public List<Policy> getAllPolicies() {
        return new ArrayList<>(policyCache.values());
    }
    
    public List<Policy> getPoliciesByType(String type) {
        return policyCache.values().stream()
                .filter(policy -> type.equals(policy.getType()))
                .sorted(Comparator.comparing(Policy::getPriority).reversed())
                .toList();
    }
    
    public Policy createPolicy(Policy policy) {
        policy.setId(UUID.randomUUID().toString());
        policy.setCreatedAt(java.time.LocalDateTime.now());
        policy.setUpdatedAt(java.time.LocalDateTime.now());
        policyCache.put(policy.getId(), policy);
        
        log.info("Created policy: {}", policy.getName());
        return policy;
    }
    
    public Policy updatePolicy(String id, Policy policy) {
        Policy existing = policyCache.get(id);
        if (existing != null) {
            policy.setId(id);
            policy.setUpdatedAt(java.time.LocalDateTime.now());
            policyCache.put(id, policy);
            log.info("Updated policy: {}", policy.getName());
            return policy;
        }
        return null;
    }
    
    public boolean deletePolicy(String id) {
        Policy removed = policyCache.remove(id);
        if (removed != null) {
            log.info("Deleted policy: {}", removed.getName());
            return true;
        }
        return false;
    }
    
    public boolean evaluatePolicy(String policyType, Map<String, Object> context) {
        List<Policy> policies = getPoliciesByType(policyType);
        
        for (Policy policy : policies) {
            if (policy.isEnabled() && evaluateRules(policy.getRules(), context)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean evaluateRules(Map<String, Object> rules, Map<String, Object> context) {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<String, Object> rule : rules.entrySet()) {
            String key = rule.getKey();
            Object expectedValue = rule.getValue();
            Object actualValue = context.get(key);
            
            if (!evaluateRule(expectedValue, actualValue)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean evaluateRule(Object expected, Object actual) {
        if (expected == null && actual == null) {
            return true;
        }
        if (expected == null || actual == null) {
            return false;
        }
        
        if (expected instanceof Map && actual instanceof Map) {
            return evaluateRules((Map<String, Object>) expected, (Map<String, Object>) actual);
        }
        
        return expected.equals(actual);
    }
    
    public Policy getCurrentPolicy() {
        // Return the highest priority enabled policy
        return policyCache.values().stream()
                .filter(Policy::isEnabled)
                .max(Comparator.comparing(Policy::getPriority))
                .orElse(getDefaultPolicy());
    }
    
    private Policy getDefaultPolicy() {
        return Policy.builder()
                .id("default")
                .name("Default Policy")
                .type("default")
                .enabled(true)
                .priority(0)
                .rules(Map.of(
                        "performanceWeight", 0.5,
                        "costWeight", 0.3,
                        "cpuThreshold", 80.0,
                        "autoOptimizationEnabled", true,
                        "region", "us-east-1"
                ))
                .build();
    }
    
    private void initializeDefaultPolicies() {
        Policy scalingPolicy = Policy.builder()
                .id("default-scaling")
                .name("Default Scaling Policy")
                .description("Default policy for autoscaling decisions")
                .type("scaling")
                .enabled(true)
                .priority(1)
                .rules(Map.of(
                        "maxScaleUpPercentage", 50,
                        "maxScaleDownPercentage", 30,
                        "cooldownMinutes", 10,
                        "minInstances", 1,
                        "maxInstances", 20
                ))
                .build();
        
        // Default cost policy
        Policy costPolicy = Policy.builder()
                .id("default-cost")
                .name("Default Cost Policy")
                .description("Default policy for cost optimization")
                .type("cost")
                .enabled(true)
                .priority(1)
                .rules(Map.of(
                        "maxMonthlyBudget", 10000.0,
                        "costAlertThreshold", 0.8,
                        "requireApprovalForSavings", 100.0
                ))
                .build();
        
        // Default security policy
        Policy securityPolicy = Policy.builder()
                .id("default-security")
                .name("Default Security Policy")
                .description("Default policy for security constraints")
                .type("security")
                .enabled(true)
                .priority(1)
                .rules(Map.of(
                        "requireApprovalForProduction", true,
                        "maxRiskLevel", "medium",
                        "auditAllActions", true
                ))
                .build();
        
        policyCache.put(scalingPolicy.getId(), scalingPolicy);
        policyCache.put(costPolicy.getId(), costPolicy);
        policyCache.put(securityPolicy.getId(), securityPolicy);
        
        log.info("Initialized {} default policies", policyCache.size());
    }
}
