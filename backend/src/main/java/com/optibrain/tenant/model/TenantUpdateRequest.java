package com.optibrain.tenant.model;

import java.util.List;

/**
 * Partial update for a tenant.
 *
 * <p>The persistence entity uses primitive booleans, so a {@code PUT} bound directly to
 * it can never tell "field absent" from "field false" and every partial body silently
 * resets approval requirements and budget. This DTO uses nullable wrappers so an update
 * touches only the fields the caller actually supplied; null means "leave unchanged".
 */
public class TenantUpdateRequest {

    private String name;
    private String plan;
    private String mode;
    private Boolean autonomousMode;
    private Boolean requireApprovalForChanges;
    private Double monthlyBudgetLimit;
    private List<String> protectedResources;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Boolean getAutonomousMode() {
        return autonomousMode;
    }

    public void setAutonomousMode(Boolean autonomousMode) {
        this.autonomousMode = autonomousMode;
    }

    public Boolean getRequireApprovalForChanges() {
        return requireApprovalForChanges;
    }

    public void setRequireApprovalForChanges(Boolean requireApprovalForChanges) {
        this.requireApprovalForChanges = requireApprovalForChanges;
    }

    public Double getMonthlyBudgetLimit() {
        return monthlyBudgetLimit;
    }

    public void setMonthlyBudgetLimit(Double monthlyBudgetLimit) {
        this.monthlyBudgetLimit = monthlyBudgetLimit;
    }

    public List<String> getProtectedResources() {
        return protectedResources;
    }

    public void setProtectedResources(List<String> protectedResources) {
        this.protectedResources = protectedResources;
    }
}