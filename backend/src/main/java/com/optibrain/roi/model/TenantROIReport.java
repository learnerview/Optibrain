package com.optibrain.roi.model;

import com.optibrain.savings.model.SavingsReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantROIReport {
    private String tenantId;
    private String tenantName;
    private LocalDateTime reportGeneratedAt;
    private LocalDateTime tenantSince;
    private String currentPlan;
    private ROIMetrics roiMetrics;
    private SavingsReport savingsReport;
    private int totalRecommendations;
    private int executedRecommendations;
    private int auditActionsCount;
    private int autonomousActionsCount;
}
