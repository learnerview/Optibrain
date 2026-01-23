package com.optibrain.autoscaling.controller;

import com.optibrain.autoscaling.service.AutoscalingService;
import com.optibrain.decision.model.Decision;
import com.optibrain.decision.scorer.DecisionScorer;
import com.optibrain.metrics.provider.MetricsProvider;
import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/autoscaling")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AutoscalingController {

    private final AutoscalingService autoscalingService;
    private final DecisionScorer decisionScorer;
    private final MetricsProvider metricsProvider;

    @PostMapping("/evaluate-and-act")
    public ApiResponse<Decision> evaluateAndAct() {
        Decision decision = decisionScorer.scoreAndDecide(metricsProvider.getCurrentMetrics().get(0));
        Decision result = autoscalingService.evaluateAndAct(decision);
        return ApiResponse.success(result, "Evaluated and acted");
    }

    @GetMapping("/status")
    public ApiResponse<String> status() {
        return ApiResponse.success("Autoscaling service active", "Status");
    }
}
