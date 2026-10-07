package com.optibrain.policy.service;

import com.optibrain.common.context.TenantContext;
import com.optibrain.policy.model.Policy;
import com.optibrain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Durable, tenant-scoped policies.
 *
 * <p>Policies were previously kept in an in-memory map, so they were shared by every
 * tenant and lost on restart. Now every read and write is scoped to the tenant on
 * {@link TenantContext}: a tenant sees only its own policies, and the default set is
 * seeded for a tenant on first use.
 *
 * <p>When no {@link TenantContext} is present (startup, tests, background tasks outside a
 * request), reads that would otherwise fail return the built-in default so the product
 * can still reason about a resource. Writes still require a tenant context, because a
 * policy with no owning tenant would be shared by accident.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policies;

    public Policy getPolicy(String id) {
        String tenantId = currentTenant();
        if (tenantId == null) {
            return null;
        }
        return policies.findByIdAndTenantId(id, tenantId).orElse(null);
    }

    public List<Policy> getAllPolicies() {
        String tenantId = currentTenant();
        if (tenantId == null) {
            return List.of();
        }
        return new ArrayList<>(policies.findByTenantIdOrderByPriorityDesc(tenantId));
    }

    public List<Policy> getPoliciesByType(String type) {
        return getAllPolicies().stream()
                .filter(policy -> type.equals(policy.getType()))
                .sorted(Comparator.comparing(Policy::getPriority).reversed())
                .toList();
    }

    @Transactional
    public Policy createPolicy(Policy policy) {
        policy.setId(UUID.randomUUID().toString());
        policy.setTenantId(requireTenant());
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());
        log.info("Created policy: {}", policy.getName());
        return policies.save(policy);
    }

    @Transactional
    public Policy updatePolicy(String id, Policy policy) {
        Policy existing = getPolicy(id);
        if (existing == null) {
            return null;
        }
        policy.setId(id);
        // The tenant is taken from the stored row, never from the caller, so an update
        // can never move a policy to another tenant.
        policy.setTenantId(existing.getTenantId());
        policy.setUpdatedAt(LocalDateTime.now());
        log.info("Updated policy: {}", policy.getName());
        return policies.save(policy);
    }

    @Transactional
    public boolean deletePolicy(String id) {
        Policy existing = getPolicy(id);
        if (existing == null) {
            return false;
        }
        policies.deleteByIdAndTenantId(id, existing.getTenantId());
        log.info("Deleted policy: {}", existing.getName());
        return true;
    }

    public boolean evaluatePolicy(String policyType, Map<String, Object> context) {
        List<Policy> matching = getPoliciesByType(policyType);
        for (Policy policy : matching) {
            if (policy.isEnabled() && evaluateRules(policy.getRules(), context)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The highest-priority enabled policy for the current tenant.
     *
     * <p>For a tenant with no policies yet, the defaults are seeded and looked up again so
     * the returned value is the durable row, not a transient object. Outside a tenant
     * context the built-in default is returned without persistence.
     */
    public Policy getCurrentPolicy() {
        String tenantId = currentTenant();
        if (tenantId == null) {
            return getDefaultPolicy();
        }
        return policies.findFirstByTenantIdAndEnabledTrueOrderByPriorityDesc(tenantId)
                .orElseGet(() -> {
                    seedDefaults(tenantId);
                    return policies.findFirstByTenantIdAndEnabledTrueOrderByPriorityDesc(tenantId)
                            .orElse(getDefaultPolicy());
                });
    }

    private void seedDefaults(String tenantId) {
        if (policies.existsByTenantId(tenantId)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Policy> defaults = List.of(
                Policy.builder()
                        .id(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .name("Default Scaling Policy")
                        .description("Default policy for autoscaling decisions")
                        .type("scaling")
                        .enabled(true)
                        .priority(1)
                        .createdAt(now)
                        .updatedAt(now)
                        .rules(Map.of(
                                "maxScaleUpPercentage", 50,
                                "maxScaleDownPercentage", 30,
                                "cooldownMinutes", 10,
                                "minInstances", 1,
                                "maxInstances", 20
                        ))
                        .build(),
                Policy.builder()
                        .id(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .name("Default Cost Policy")
                        .description("Default policy for cost optimization")
                        .type("cost")
                        .enabled(true)
                        .priority(1)
                        .createdAt(now)
                        .updatedAt(now)
                        .rules(Map.of(
                                "maxMonthlyBudget", 10000.0,
                                "costAlertThreshold", 0.8,
                                "requireApprovalForSavings", 100.0
                        ))
                        .build(),
                Policy.builder()
                        .id(UUID.randomUUID().toString())
                        .tenantId(tenantId)
                        .name("Default Security Policy")
                        .description("Default policy for security constraints")
                        .type("security")
                        .enabled(true)
                        .priority(1)
                        .createdAt(now)
                        .updatedAt(now)
                        .rules(Map.of(
                                "requireApprovalForProduction", true,
                                "maxRiskLevel", "medium",
                                "auditAllActions", true
                        ))
                        .build());
        policies.saveAll(defaults);
        log.info("Seeded {} default policies for tenant {}", defaults.size(), tenantId);
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
            Map<?, ?> expectedMap = (Map<?, ?>) expected;
            Map<?, ?> actualMap = (Map<?, ?>) actual;
            if (!hasOnlyStringKeys(expectedMap) || !hasOnlyStringKeys(actualMap)) {
                log.warn("Policy rule has non-String keys; treating as a plain value comparison");
                return expected.equals(actual);
            }
            return evaluateRules(toStringKeyedMap(expectedMap), toStringKeyedMap(actualMap));
        }
        return expected.equals(actual);
    }

    private boolean hasOnlyStringKeys(Map<?, ?> map) {
        return map.keySet().stream().allMatch(String.class::isInstance);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toStringKeyedMap(Map<?, ?> map) {
        // Safe: the caller has verified every key is a String.
        return (Map<String, Object>) map;
    }

    /** The tenant on the thread, or null when there is none (ok for reads). */
    private String currentTenant() {
        String tenantId = TenantContext.getTenantId();
        return tenantId == null || tenantId.isBlank() ? null : tenantId;
    }

    private String requireTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException(
                    "Policies require a tenant context; none is set on this thread");
        }
        return tenantId;
    }
}