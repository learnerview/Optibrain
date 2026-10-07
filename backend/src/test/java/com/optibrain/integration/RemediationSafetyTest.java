package com.optibrain.integration;

import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.remediator.CloudRemediator;
import com.optibrain.decision.model.Decision;
import com.optibrain.decision.scorer.DecisionScorer;
import com.optibrain.metrics.model.MetricData;
import com.optibrain.policy.model.Policy;
import com.optibrain.policy.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Decision scoring must never act on a resource it cannot identify.
 *
 * <p>This replaces an integration test that depended on a deleted mock metrics
 * provider. It exercises the property that actually matters: a metric with no
 * {@code InstanceId} is scored and explained, but never remediated.
 */
class RemediationSafetyTest {

    private PolicyService policyService;
    private CloudRemediator remediator;
    private DecisionScorer scorer;

    private static Policy policy(boolean autoOptimize) {
        return Policy.builder()
                .rules(Map.of(
                        "performanceWeight", 0.7,
                        "costWeight", 0.3,
                        "cpuThreshold", 80.0,
                        "autoOptimizationEnabled", autoOptimize))
                .build();
    }

    private static MetricData metric(String instanceId, double cpu) {
        Map<String, String> dimensions = instanceId == null
                ? Map.of()
                : Map.of("InstanceId", instanceId);
        return MetricData.builder()
                .cpuUtilization(cpu)
                .hourlyCost(1.0)
                .region("us-east-1")
                .dimensions(dimensions)
                .build();
    }

    @BeforeEach
    void setUp() {
        policyService = mock(PolicyService.class);
        remediator = mock(CloudRemediator.class);
        scorer = new DecisionScorer(policyService, remediator);
    }

    @Test
    @DisplayName("does not remediate when the metric carries no InstanceId")
    void neverActsOnUnidentifiedResource() {
        when(policyService.getCurrentPolicy()).thenReturn(policy(true));

        Decision decision = scorer.scoreAndDecide(metric(null, 99.0));

        assertNotNull(decision.getDecisionId());
        assertEquals("SCALE_UP", decision.getAction());
        assertEquals("unattributed", decision.getResourceId());
        verify(remediator, never()).executeScaleUp(anyString(), anyString());
        verify(remediator, never()).executeScaleDown(anyString(), anyString());
    }

    @Test
    @DisplayName("remediates the identified instance when auto-optimization is enabled")
    void actsOnIdentifiedResource() {
        when(policyService.getCurrentPolicy()).thenReturn(policy(true));
        when(remediator.executeScaleUp(any(), anyString()))
                .thenReturn(ActionResult.succeeded(
                        ResourceAction.of(ActionType.START_INSTANCE, "i-0123456789abcdef0", false, "test"),
                        "started", null));

        Decision decision = scorer.scoreAndDecide(metric("i-0123456789abcdef0", 99.0));

        assertEquals("i-0123456789abcdef0", decision.getResourceId());
        verify(remediator).executeScaleUp("us-east-1", "i-0123456789abcdef0");
    }

    @Test
    @DisplayName("never remediates when auto-optimization is disabled")
    void neverActsWhenPolicyDisablesAutoOptimization() {
        when(policyService.getCurrentPolicy()).thenReturn(policy(false));

        scorer.scoreAndDecide(metric("i-0123456789abcdef0", 99.0));

        verify(remediator, never()).executeScaleUp(any(), anyString());
        verify(remediator, never()).executeScaleDown(any(), anyString());
    }

    @Test
    @DisplayName("classifies destructive actions as destructive")
    void destructiveActionsAreFlagged() {
        assertEquals(true, ActionType.TERMINATE_INSTANCE.isDestructive());
        assertEquals(true, ActionType.DELETE_VOLUME.isDestructive());
        assertEquals(false, ActionType.STOP_INSTANCE.isDestructive());
        assertEquals(false, ActionType.APPLY_TAGS.isDestructive());
    }
}