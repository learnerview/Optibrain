package com.optibrain.decision.scorer;

import com.optibrain.decision.model.Decision;
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
}
