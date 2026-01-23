package com.optibrain.prediction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Forecast {
    private double predictedCpu;
    private double predictedCost;
    private double confidence;
    private String timeframe; // e.g., "+1 Hour"
}
