package com.optibrain.savings.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsReport {
    private LocalDateTime generatedAt;
    private double currentHourlyCost;
    private double currentMonthlyCost;
    private double potentialMonthlySavings;
    private double realizedMonthlySavings;
    private double potentialUpfrontCost;
    private Double potentialAvgPaybackDays;
    private Double potentialAvgRoiMonthly;
    private double realizedUpfrontCost;
    private Double realizedAvgPaybackDays;
    private Double realizedAvgRoiMonthly;
    private int totalRecommendations;
    private int pendingRecommendations;
    
    // RI & Savings Plans
    private Double riCoveragePercentage;
    private double potentialRISavingsMonthly;
    private double potentialSPSavingsMonthly;
}
