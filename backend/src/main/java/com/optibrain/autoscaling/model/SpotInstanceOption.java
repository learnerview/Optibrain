package com.optibrain.autoscaling.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotInstanceOption {
    private String instanceType;
    private String region;
    private double spotPrice;
    private double onDemandPrice;
    private double savingsPercentage;
    private double interruptionRisk; // 0.0 to 1.0
}
