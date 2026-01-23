package com.optibrain.decision.model;

import com.optibrain.security.enums.Role;
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
public class DecisionRequest {
    
    private String tenantId;
    private String userId;
    private Role userRole;
    private DecisionType decisionType;
    private Map<String, Object> currentMetrics;
    private List<Double> historicalData;
    private Map<String, Object> context;
    private LocalDateTime timestamp;
    private Map<String, Object> constraints;
    private String requestId;
    
    // Bounds that can be specified per request
    private Double minConfidence;
    private Double maxRiskLevel;
    private Boolean requireApproval;
    private Double maxExpectedCost;
}
