package com.optibrain.decision.controller;

import com.optibrain.decision.model.Decision;
import com.optibrain.decision.scorer.DecisionScorer;
import com.optibrain.metrics.provider.MetricsProvider;
import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import com.optibrain.metrics.model.MetricData;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/decisions")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class DecisionController {

    private final DecisionScorer decisionScorer; // Renamed from strategy to scorer
    private final MetricsProvider metricsProvider; 

    @GetMapping("/recent")
    public ApiResponse<Decision> getRecentDecision() {
        Decision decision = Decision.builder()
                .decisionId("DEC-999")
                .action("NONE")
                .reason("Monitoring active")
                .confidence(1.0)
                .mode("IDLE")
                .build();
        return ApiResponse.success(decision, "Recent decision retrieved");
    }

    @GetMapping("/evaluate")
    public ApiResponse<Decision> evaluateDecision() {
        Decision decision = decisionScorer.scoreAndDecide(metricsProvider.getCurrentMetrics().get(0));
        return ApiResponse.success(decision, "Decision evaluated");
    }
}
