package com.optibrain.decision.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Decision {
    
    private String id;
    private String tenantId;
    private String userId;
    private DecisionType decisionType;
    private String action;
    private String description;
    private double confidence;
    private double expectedSavings;
    public double getSavings() { return expectedSavings; }
    private String riskLevel;
    private boolean requiresApproval;
    private String status; // PENDING, APPROVED, REJECTED, EXECUTED, FAILED
    private LocalDateTime generatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime executedAt;
    private String approvedBy;
    private String executedBy;
    private Map<String, Object> parameters;
    private Map<String, Object> results;
    private String errorMessage;
    private String feedback;
    private double actualSavings;
    private boolean autonomousExecution;
    private String mlModelVersion;
    private String ruleVersion;
    
    // Legacy fields for compatibility
    private String decisionId;
    private String reason;
    private double score;
    private List<String> explanation;
    private String mode; // SIMULATION, AUTONOMOUS
    private String executionStatus;
    private Double actualSavingsLegacy;
    private String rollbackPlan;
    private String region;
    private String resourceId; // Resource identifier for operations
    
    // Decision bounds that were applied
    private Double minConfidenceApplied;
    private Double maxRiskApplied;
    private Double maxCostApplied;
}
