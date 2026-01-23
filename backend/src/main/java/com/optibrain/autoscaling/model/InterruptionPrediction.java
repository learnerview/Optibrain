package com.optibrain.autoscaling.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterruptionPrediction {
    private String instanceType;
    private String region;
    private double riskScore; // 0.0 (low) to 1.0 (very high)
    private Integer estimatedMinutesUntilInterruption;
    private Instant predictionTimestamp;
    private String recommendation; // SUSTAIN, COOLDOWN, EVACUATE
}
