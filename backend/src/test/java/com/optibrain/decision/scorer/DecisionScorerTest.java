package com.optibrain.decision.scorer;

import com.optibrain.decision.model.Decision;
import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.remediator.CloudRemediator;
import com.optibrain.metrics.model.MetricData;
import com.optibrain.policy.model.Policy;
import com.optibrain.policy.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DecisionScorerTest {

    @Mock
    private PolicyService policyService;

    @Mock
    private CloudRemediator remediator;

    @InjectMocks
    private DecisionScorer decisionScorer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        Policy mockPolicy = Policy.builder()
                .id("test-policy")
                .name("Test Policy")
                .type("test")
                .enabled(true)
                .rules(Map.of(
                    "cpuThreshold", 70.0,
                    "costWeight", 0.4,
                    "performanceWeight", 0.6,
                    "scaleUpCooldown", 10,
                    "riskTolerance", "MEDIUM"
                ))
                .build();
        when(policyService.getCurrentPolicy()).thenReturn(mockPolicy);
        ActionResult ok = ActionResult.succeeded(
                ResourceAction.of(ActionType.STOP_INSTANCE, "i-x", true, "test"),
                "simulated", 0.0);
        when(remediator.executeScaleUp(anyString(), anyString())).thenReturn(ok);
        when(remediator.executeScaleDown(anyString(), anyString())).thenReturn(ok);
    }

    @Test
    void testScaleUpDecision() {
        MetricData metrics = MetricData.builder()
                .cpuUtilization(90.0)
                .hourlyCost(100.0)
                .timestamp(Instant.now())
                .build();

        Decision decision = decisionScorer.scoreAndDecide(metrics);

        assertEquals("SCALE_UP", decision.getAction());
        assertTrue(decision.getScore() > 20.0);
        assertNotNull(decision.getExplanation());
        assertFalse(decision.getExplanation().isEmpty());
    }

    @Test
    void testScaleDownDecision() {
        MetricData metrics = MetricData.builder()
                .cpuUtilization(10.0)
                .hourlyCost(400.0)
                .timestamp(Instant.now())
                .build();

        Decision decision = decisionScorer.scoreAndDecide(metrics);

        assertEquals("SCALE_DOWN", decision.getAction());
        assertTrue(decision.getScore() < -20.0);
    }

    @Test
    void testNoActionDecision() {
        MetricData metrics = MetricData.builder()
                .cpuUtilization(50.0)
                .hourlyCost(200.0)
                .timestamp(Instant.now())
                .build();

        Decision decision = decisionScorer.scoreAndDecide(metrics);

        assertEquals("NONE", decision.getAction());
    }

    @Test
    void historyIsIsolatedPerResourceAndConfidenceIsEvidenceDriven() {
        MetricData busy = MetricData.builder()
                .cpuUtilization(95.0)
                .hourlyCost(100.0)
                .timestamp(Instant.now())
                .dimensions(Map.of("InstanceId", "i-busy"))
                .build();
        MetricData quiet = MetricData.builder()
                .cpuUtilization(60.0)
                .hourlyCost(200.0)
                .timestamp(Instant.now())
                .dimensions(Map.of("InstanceId", "i-quiet"))
                .build();

        decisionScorer.scoreAndDecide(busy);
        decisionScorer.scoreAndDecide(busy);
        decisionScorer.scoreAndDecide(busy);

        // Under a global history, i-quiet's first reading would already see i-busy's
        // values and report a downward trend. With per-resource history it has exactly
        // one sample and stays STABLE.
        Decision quietDecision = decisionScorer.scoreAndDecide(quiet);

        assertTrue(quietDecision.getExplanation().stream()
                .anyMatch(e -> e.contains("Trend: STABLE")));
        assertEquals("i-quiet", quietDecision.getResourceId());
        assertEquals(0.6, quietDecision.getConfidence(), 0.001,
                "attributed + stable trend scores 0.6, never a hardcoded 0.95");
    }
}
