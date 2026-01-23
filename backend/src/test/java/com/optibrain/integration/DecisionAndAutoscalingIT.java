package com.optibrain.integration;

import com.optibrain.autoscaling.service.AutoscalingService;
import com.optibrain.decision.model.Decision;
import com.optibrain.decision.scorer.DecisionScorer;
import com.optibrain.metrics.model.MetricData;
import com.optibrain.metrics.provider.MockMetricsProvider;
import com.optibrain.recommendation.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DecisionAndAutoscalingIT {

    @Autowired
    DecisionScorer decisionScorer;

    @Autowired
    AutoscalingService autoscalingService;

    @Autowired
    MockMetricsProvider metricsProvider;

    @Test
    void shouldEvaluateAndActWithSafety() {
        MetricData metrics = metricsProvider.getCurrentMetrics().get(0);
        Decision decision = decisionScorer.scoreAndDecide(metrics);

        assertNotNull(decision.getDecisionId());
        assertNotNull(decision.getAction());
        assertNotNull(decision.getReason());

        Decision result = autoscalingService.evaluateAndAct(decision);
        assertEquals(decision.getDecisionId(), result.getDecisionId());
        assertTrue(result.getMode().equals("DRY_RUN") || result.getMode().equals("EXECUTED"));
    }

    @Test
    void shouldGenerateRecommendations() {
        MetricData metrics = metricsProvider.getCurrentMetrics().get(0);
        var recs = recommendationService.generateRightsizing(metrics);
        assertNotNull(recs);
    }

    @Autowired
    RecommendationService recommendationService;
}
