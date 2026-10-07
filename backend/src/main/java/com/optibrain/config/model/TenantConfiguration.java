package com.optibrain.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfiguration {
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
