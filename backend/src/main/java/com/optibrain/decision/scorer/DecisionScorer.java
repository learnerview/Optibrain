package com.optibrain.decision.scorer;

import com.optibrain.cloud.model.ActionResult;
import com.optibrain.common.context.TenantContext;
import com.optibrain.decision.model.Decision;
import com.optibrain.metrics.model.MetricData;
import com.optibrain.policy.model.Policy;
import com.optibrain.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DecisionScorer {

    private final PolicyService policyService;
    private final com.optibrain.cloud.remediator.CloudRemediator remediator;

    /**
     * Trend history is per decision subject, keyed by tenant and resource, never global.
     * A shared list would let one tenant's or resource's CPU observations move the trend
     * read for a different resource, which is exactly the cross-contamination the multi-
     * tenant design must avoid.
     */
    private final Map<String, Deque<Double>> cpuHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 5;

    public Decision scoreAndDecide(MetricData metrics) {
        Policy policy = policyService.getCurrentPolicy();

        List<String> explanation = new ArrayList<>();

        // Autonomous execution is only safe when the metric actually identifies a
        // resource. Acting on a placeholder id would issue an AWS call against a
        // resource that does not exist, so an unattributed metric only gets scored.
        String instanceId = instanceIdOf(metrics);

        // 1. Update History & Calculate Trend (scoped to this tenant:resource)
        double cpuFactor = metrics.getCpuUtilization();
        String subjectKey = historyKey(TenantContext.getTenantId(), instanceId);
        updateHistory(subjectKey, cpuFactor);
        String trend = calculateTrend(subjectKey);

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
                    if (instanceId == null) {
                        executionStatus = "SKIPPED";
                        explanation.add("Auto-optimization skipped: metric carries no InstanceId");
                    } else {
                        ActionResult result = remediator.executeScaleUp(policy.getRegion(), instanceId);
                        executionStatus = result.success() ? "EXECUTED" : "FAILED";
                        explanation.add("Action executed autonomously via CloudRemediator");
                    }
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
                    if (instanceId == null) {
                        executionStatus = "SKIPPED";
                        explanation.add("Auto-optimization skipped: metric carries no InstanceId");
                    } else {
                        ActionResult result = remediator.executeScaleDown(policy.getRegion(), instanceId);
                        executionStatus = result.success() ? "EXECUTED" : "FAILED";
                        explanation.add("Action executed autonomously via CloudRemediator");
                    }
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
                .confidence(confidenceOf(trend, instanceId != null))
                .mode(policy.isAutoOptimizationEnabled() ? "AUTONOMOUS_ACTIVE" : "AUTONOMOUS_SCORER")
                // Previously computed but never written to the Decision, so the outcome of
                // an autonomous action was silently dropped.
                .executionStatus(executionStatus)
                .region(metrics.getRegion())
                .resourceId(instanceId == null ? "unattributed" : instanceId)
                .build();
    }

    /** The CloudWatch dimension that identifies the instance this metric belongs to. */
    private String instanceIdOf(MetricData metrics) {
        if (metrics.getDimensions() == null) {
            return null;
        }
        String id = metrics.getDimensions().get("InstanceId");
        return id == null || id.isBlank() ? null : id;
    }

    private static String historyKey(String tenantId, String resourceId) {
        return (tenantId == null || tenantId.isBlank() ? "untracked" : tenantId) + ":"
                + (resourceId == null ? "unattributed" : resourceId);
    }

    private void updateHistory(String key, double cpu) {
        Deque<Double> history = cpuHistory.computeIfAbsent(key, k -> new ArrayDeque<>());
        if (history.size() >= MAX_HISTORY) {
            history.removeFirst();
        }
        history.addLast(cpu);
    }

    private String calculateTrend(String key) {
        Deque<Double> history = cpuHistory.get(key);
        if (history == null || history.size() < 3) return "STABLE";
        double first = history.getFirst();
        double last = history.getLast();
        if (last > first + 5) return "UPWARD";
        if (last < first - 5) return "DOWNWARD";
        return "STABLE";
    }

    /**
     * A heuristic, not a statistical measure. The decision surface has no probabilistic
     * model, so a fixed value like 0.95 would assert a precision the code never
     * computes. The value is driven by what is actually observable here: whether the
     * metric names a concrete resource, and whether enough history exists to read a
     * clear trend.
     */
    private double confidenceOf(String trend, boolean attributed) {
        if ("STABLE".equals(trend)) {
            return attributed ? 0.6 : 0.5;
        }
        return attributed ? 0.9 : 0.7;
    }
}
