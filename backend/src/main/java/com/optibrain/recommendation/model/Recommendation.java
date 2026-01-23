package com.optibrain.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    private String id;
    private String resourceId;
    private String currentType;
    private String recommendedType;
    private String action; // RIGHTSIZE, SCALE_UP, SCALE_DOWN, SCHEDULED_STOP, DECOMMISSION, ORPHAN_CLEANUP, RI_OPTIMIZATION, SP_OPTIMIZATION
    private double currentHourlyCost;
    private double recommendedHourlyCost;
    private double monthlySavings;
    private double upfrontCost;
    private Double roiMonthly;
    private Integer paybackDays;
    private Double confidence;
    private double utilizationCpu;
    private double utilizationMemory;
    private String reason;
    private Map<String, String> details;
    private String status; // PENDING, APPROVED, REJECTED, EXECUTED
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
}
