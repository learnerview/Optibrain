package com.optibrain.policy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * An optimization policy, persisted and scoped to the tenant that owns it.
 *
 * <p>Previously this was a POJO held in an in-memory map in {@code PolicyService}, so
 * every process start discarded changes and all tenants shared one policy set. Now the
 * {@code tenantId} column scopes policies per tenant and the repository makes them
 * durable. {@link PolicyService} seeds the default set for a tenant on first use.
 *
 * <p>{@code rules} has no natural column type, so it is stored as JSON with
 * {@link JsonStringMapConverter}.
 */
@Entity
@Table(name = "policies", indexes = {
        @Index(name = "idx_policies_tenant", columnList = "tenantId, enabled, priority")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Policy {

    @Id
    @Column(nullable = false, length = 64)
    private String id;

    /** The tenant that owns this policy. Policies are never shared across tenants. */
    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, length = 64)
    private String type;

    @Convert(converter = JsonStringMapConverter.class)
    @Column(nullable = false, length = 8000)
    private Map<String, Object> rules;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int priority;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 128)
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