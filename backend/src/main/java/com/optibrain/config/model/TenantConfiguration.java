package com.optibrain.config.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-tenant tuning knobs, keyed by tenant id.
 *
 * <p>Persisted rather than rebuilt from constants on every read: a threshold an operator
 * changes must survive the next request and reach the services that consume it.
 */
@Entity
@Table(name = "tenant_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfiguration {
    @Id
    private String tenantId;
    private double cpuUpperBound;
    private double cpuLowerBound;
    private String scalingStrategy;
    private boolean enableML;
    private double riskThreshold;
    private int riPreferredTerm;
    private String riPaymentOption;
    private int ebsUnusedDaysThreshold;
    private int snapshotOldDaysThreshold;
    private boolean autoCleanupEnabled;
    @Default
    private double riPreferredTermValue = 1; // 1 year default
}
