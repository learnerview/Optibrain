package com.optibrain.roi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ROIMetrics {
    private double totalInvestment;
    private double totalSavings;
    private double monthlySavings;
    private double annualSavings;
    private double roiPercentage;
    private double averagePaybackDays;
    private String trend; // EXCELLENT, GOOD, STABLE, POOR
    private LocalDateTime calculatedAt;
}
