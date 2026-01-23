package com.optibrain.decision.model;

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
public class DecisionResponse {
    
    private Decision decision;
    private Map<String, Object> mlRecommendations;
    private Map<String, Object> ruleRecommendations;
    private boolean requiresApproval;
    private double confidence;
    private String riskAssessment;
    private LocalDateTime generatedAt;
    private String executionId;
    private String errorMessage;
    
    // Additional metadata for frontend
    private Map<String, Object> metadata;
    private boolean canExecute;
    private boolean canApprove;
    private boolean canReject;
    private String nextAction;
}
