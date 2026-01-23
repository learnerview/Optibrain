package com.optibrain.decision.scorer;

import com.optibrain.decision.model.Decision;
import com.optibrain.metrics.model.MetricData;
import com.optibrain.policy.model.Policy;
import com.optibrain.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DecisionScorer {

    private final PolicyService policyService;
    private final com.optibrain.cloud.remediator.CloudRemediator remediator;
    private final List<Double> cpuHistory = new LinkedList<>();
    private static final int MAX_HISTORY = 5;

    public Decision scoreAndDecide(MetricData metrics) {
        Policy policy = policyService.getCurrentPolicy();
        
        List<String> explanation = new ArrayList<>();
        
        // 1. Update History & Calculate Trend
        double cpuFactor = metrics.getCpuUtilization();
        updateHistory(cpuFactor);
        String trend = calculateTrend();
        
        // 2. Calculate Factors Based on Policy Weights
        double costFactor = (metrics.getHourlyCost() / 500.0) * 100; // Normalized cost

        double score = (cpuFactor * policy.getPerformanceWeight()) - (costFactor * policy.getCostWeight());
        
        explanation.add(String.format("Performance weight (%.1f) vs Cost weight (%.1f)", 
                policy.getPerformanceWeight(), policy.getCostWeight()));
        explanation.add(String.format("Current CPU Utilization: %.1f%% (Trend: %s)", cpuFactor, trend));
        explanation.add(String.format("Current Hourly Cost mapped to factor: %.1f", costFactor));

        String action = "NONE";
        String reason = "System operating within optimal parameters";
        String executionStatus = "SIMULATED";
        
        // thresholds are now policy driven
        double scaleUpThreshold = policy.getCpuThreshold() - 50; 
        double scaleDownThreshold = -20; // Default buffer, could also be dynamic

        // 3. Decision Logic with Trend Awareness
        if (score > scaleUpThreshold) {
            if ("UPWARD".equals(trend) || cpuFactor > policy.getCpuThreshold()) {
                action = "SCALE_UP";
                reason = String.format("Performance Score %.2f exceeded threshold with Upward CPU Trend (Threshold: %.1f%%)", score, policy.getCpuThreshold());
                explanation.add(String.format("Score %.2f > Threshold %.2f and trend is %s", score, scaleUpThreshold, trend));
                
                if (policy.isAutoOptimizationEnabled()) {
                    boolean success = remediator.executeScaleUp(policy.getRegion(), "auto-scale-up");
                    executionStatus = success ? "EXECUTED" : "FAILED";
                    explanation.add("Action executed autonomously via CloudRemediator");
                }
            } else {
                reason = "Threshold exceeded but trend is stable/downward (Suppressing Scale-Up)";
            }
        } else if (score < scaleDownThreshold) {
            if ("DOWNWARD".equals(trend) || cpuFactor < 20) {
                action = "SCALE_DOWN";
                reason = "Cost efficiency opportunity detected with Downward CPU Trend";
                explanation.add(String.format("Score %.2f < Threshold %.2f and trend is %s", score, scaleDownThreshold, trend));
                
                if (policy.isAutoOptimizationEnabled()) {
                    boolean success = remediator.executeScaleDown(policy.getRegion(), "auto-scale-down");
                    executionStatus = success ? "EXECUTED" : "FAILED";
                    explanation.add("Action executed autonomously via CloudRemediator");
                }
            } else {
                reason = "Efficiency detected but trend is rising/stable (Holding resources)";
            }
        }

        return Decision.builder()
                .decisionId(UUID.randomUUID().toString().substring(0, 8))
                .action(action)
                .reason(reason)
                .score(score)
                .explanation(explanation)
                .confidence(0.95)
                .mode(policy.isAutoOptimizationEnabled() ? "AUTONOMOUS_ACTIVE" : "AUTONOMOUS_SCORER")
                .region(metrics.getRegion())
                .resourceId("auto-generated-resource")
                .build();
    }

    private void updateHistory(double cpu) {
        if (cpuHistory.size() >= MAX_HISTORY) {
            cpuHistory.remove(0);
        }
        cpuHistory.add(cpu);
    }

    private String calculateTrend() {
        if (cpuHistory.size() < 3) return "STABLE";
        double first = cpuHistory.get(0);
        double last = cpuHistory.get(cpuHistory.size() - 1);
        if (last > first + 5) return "UPWARD";
        if (last < first - 5) return "DOWNWARD";
        return "STABLE";
    }
}
