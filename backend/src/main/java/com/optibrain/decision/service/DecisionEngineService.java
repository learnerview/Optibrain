package com.optibrain.decision.service;

import com.optibrain.chat.service.PyBridgeService;

import com.optibrain.decision.model.Decision;
import com.optibrain.decision.model.DecisionRequest;
import com.optibrain.decision.model.DecisionResponse;
import com.optibrain.decision.model.DecisionType;
import com.optibrain.recommendation.model.Recommendation;
import com.optibrain.security.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionEngineService {
    
    private final PyBridgeService mlService;
    private final com.optibrain.config.service.TenantConfigurationService configService;
    
    public DecisionResponse makeDecision(DecisionRequest request) {
        // ... (existing logic)
        try {
            var config = configService.getConfiguration(request.getTenantId());
            
            // Get ML recommendations if enabled (assuming enabled by default or added to config)
            // For now assuming true as it's not in TenantConfiguration yet, or use default
            boolean enableML = true; 
            Map<String, Object> mlRecommendations = null;
            if (enableML) {
                mlRecommendations = getMLRecommendations(request);
            }
            
            // Get rule-based recommendations
            Map<String, Object> ruleRecommendations = getRuleBasedRecommendations(request, config);
            
            // Combine recommendations
            Decision decision = combineRecommendations(request, mlRecommendations, ruleRecommendations);
            
            // Apply bounds and constraints
            decision = applyBoundsAndConstraints(decision, request, config);
            
            // Determine if approval is needed
            boolean requiresApproval = requiresApproval(decision, request.getUserRole());
            
            // ... (rest of logic)
            DecisionResponse response = DecisionResponse.builder()
                    .decision(decision)
                    // ...
                    .confidence(calculateConfidence(decision, mlRecommendations, ruleRecommendations))
                    .riskAssessment(assessRisk(decision))
                    .generatedAt(LocalDateTime.now())
                    .build();
            return response;

        } catch (Exception e) {
            log.error("Error making decision: {}", e.getMessage());
            return createFallbackDecision(request, e);
        }
    }

    private Map<String, Object> getRuleBasedRecommendations(DecisionRequest request, com.optibrain.config.model.TenantConfiguration config) {
        return switch (request.getDecisionType()) {
            case COST_OPTIMIZATION -> getCostOptimizationRules(request, config);
            case FORECASTING -> getForecastingRules(request);
            case SCALING -> getScalingRules(request, config);
            default -> Map.of();
        };
    }

    private Map<String, Object> getCostOptimizationRules(DecisionRequest request, com.optibrain.config.model.TenantConfiguration config) {
        double cpuUtil = ((Number) request.getCurrentMetrics().getOrDefault("cpu_utilization", 0.0)).doubleValue();
        double hourlyCost = ((Number) request.getCurrentMetrics().getOrDefault("hourly_cost", 0.0)).doubleValue();
        
        // Use dynamic config
        double scaleUpThreshold = config.getCpuUpperBound();
        double scaleDownThreshold = config.getCpuLowerBound();
        
        String action = "NO_ACTION";
        double expectedSavings = 0.0;
        String reason = "System operating within normal bounds";
        
        if (cpuUtil < scaleDownThreshold) {
            action = "RIGHTSIZE_DOWN";
            expectedSavings = hourlyCost * 0.3;
            reason = "Low utilization detected (< " + scaleDownThreshold + "%), recommend downsizing";
        } else if (cpuUtil > scaleUpThreshold) {
            action = "RIGHTSIZE_UP";
            expectedSavings = -hourlyCost * 0.2;
            reason = "High utilization detected (> " + scaleUpThreshold + "%), recommend upsizing";
        }
        
        return Map.of(
            "action", action,
            "expectedSavings", expectedSavings,
            "confidence", 0.8,
            "reason", reason
        );
    }
    
    private Map<String, Object> getScalingRules(DecisionRequest request, com.optibrain.config.model.TenantConfiguration config) {
        double cpuUtil = ((Number) request.getCurrentMetrics().getOrDefault("cpu_utilization", 0.0)).doubleValue();
        
        String action = "NO_ACTION";
        if (cpuUtil > config.getCpuUpperBound()) {
            action = "SCALE_UP";
        } else if (cpuUtil < config.getCpuLowerBound()) {
            action = "SCALE_DOWN";
        }
        
        return Map.of(
            "action", action,
            "confidence", 0.7,
            "cpuUtilization", cpuUtil
        );
    }

    private Decision combineRecommendations(DecisionRequest request, Map<String, Object> mlRecs, Map<String, Object> ruleRecs) {
        // ... (simplified logic)
        String action = "NO_ACTION";
        if (mlRecs != null && !mlRecs.isEmpty()) {
            action = (String) mlRecs.getOrDefault("action", "NO_ACTION");
        } else if (ruleRecs != null && !ruleRecs.isEmpty()) {
            action = (String) ruleRecs.getOrDefault("action", "NO_ACTION");
        }
        
        return Decision.builder()
                .id(java.util.UUID.randomUUID().toString())
                .tenantId(request.getTenantId())
                .decisionType(request.getDecisionType())
                .action(action)
                .confidence(0.8) // Simplified
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private Decision applyBoundsAndConstraints(Decision decision, DecisionRequest request, com.optibrain.config.model.TenantConfiguration config) {
        // Apply logic based on config.getScalingStrategy() if needed
        return decision;
    }
    
    // ... (keep other methods but remove DynamicConfig usage)

    
    private boolean requiresApproval(Decision decision, Role userRole) {
        var config = configService.getConfiguration("default"); // Use default tenant or pass from request
        
        // Always require approval for viewers
        if (userRole == Role.VIEWER_ONLY) {
            return true;
        }
        
        // Require approval for high-risk decisions
        if (decision.getRiskLevel() == "HIGH") {
            return true;
        }
        
        // Require approval based on confidence threshold
        return decision.getConfidence() < 0.7; // Default threshold
    }
    
    private double calculateConfidence(Decision decision, 
                                   Map<String, Object> mlRecs, 
                                   Map<String, Object> ruleRecs) {
        double mlConfidence = mlRecs != null ? 
                ((Number) mlRecs.getOrDefault("confidence", 0.5)).doubleValue() : 0.0;
        double ruleConfidence = ruleRecs != null ? 
                ((Number) ruleRecs.getOrDefault("confidence", 0.5)).doubleValue() : 0.0;
        
        // Use default weights
        double mlWeight = 0.6;
        double ruleWeight = 0.4;
        
        return (mlConfidence * mlWeight) + (ruleConfidence * ruleWeight);
    }
    
    private String assessRisk(Decision decision) {
        return assessRiskFromConfidence(decision.getConfidence());
    }
    
    private String assessRiskFromConfidence(double confidence) {
        // Use default risk threshold
        double riskThreshold = 0.6;
        
        if (confidence > 0.8) {
            return "LOW";
        } else if (confidence > riskThreshold) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }
    
    private DecisionResponse createFallbackDecision(DecisionRequest request, Exception e) {
        Decision fallbackDecision = Decision.builder()
                .id(java.util.UUID.randomUUID().toString())
                .tenantId(request.getTenantId())
                .decisionType(request.getDecisionType())
                .action("NO_ACTION")
                .confidence(0.0)
                .expectedSavings(0.0)
                .riskLevel("HIGH")
                .requiresApproval(true)
                .errorMessage("Decision engine failed: " + e.getMessage())
                .generatedAt(LocalDateTime.now())
                .build();
        
        return DecisionResponse.builder()
                .decision(fallbackDecision)
                .requiresApproval(true)
                .confidence(0.0)
                .riskAssessment("HIGH")
                .generatedAt(LocalDateTime.now())
                .build();
    }
    
    private Map<String, Object> getMLRecommendations(DecisionRequest request) {
        try {
            return switch (request.getDecisionType()) {
                case FORECASTING -> mlService.getForecastForScaling(
                    request.getTenantId(), 
                    "cpu", 
                    request.getHistoricalData()
                );
                case COST_OPTIMIZATION -> mlService.getCostOptimization(request.getCurrentMetrics());
                case SCALING -> mlService.getForecastForScaling(
                    request.getTenantId(), 
                    "cpu", 
                    request.getHistoricalData()
                );
                default -> Map.of();
            };
        } catch (Exception e) {
            log.warn("Failed to get ML recommendations: {}", e.getMessage());
            return Map.of();
        }
    }
    
    private Map<String, Object> getForecastingRules(DecisionRequest request) {
        return Map.of(
            "recommendation", "Rule-based forecasting",
            "confidence", 0.7,
            "action", "ANALYZE"
        );
    }
    
    public Map<String, Object> executeDecision(String tenantId, String decisionType) {
        DecisionRequest request = DecisionRequest.builder()
            .tenantId(tenantId)
            .decisionType(DecisionType.valueOf(decisionType))
            .build();
            
        DecisionResponse response = makeDecision(request);
        return Map.of(
            "decision", response.getDecision(),
            "confidence", response.getConfidence(),
            "status", "EXECUTED"
        );
    }
}
