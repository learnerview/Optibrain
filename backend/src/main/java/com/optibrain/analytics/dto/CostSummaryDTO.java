package com.optibrain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CostSummaryDTO {
    private double totalSaved;
    private double potentialMonthlySavings;
    private double savingsPercentage;
    private int activeAutomations;
}
