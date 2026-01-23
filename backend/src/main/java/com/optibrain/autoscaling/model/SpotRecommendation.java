package com.optibrain.autoscaling.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotRecommendation {
    private String tenantId;
    private List<SpotInstanceOption> options;
    private double totalPotentialMonthlySavings;
    private String recommendationReason;
}
