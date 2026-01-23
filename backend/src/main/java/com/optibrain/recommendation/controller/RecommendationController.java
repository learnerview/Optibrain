package com.optibrain.recommendation.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.metrics.provider.MetricsProvider;
import com.optibrain.recommendation.model.Recommendation;
import com.optibrain.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final MetricsProvider metricsProvider;

    @PostMapping("/generate")
    public ApiResponse<List<Recommendation>> generate() {
        var metrics = metricsProvider.getCurrentMetrics().get(0);
        List<Recommendation> recs = recommendationService.generateRightsizing(metrics);
        return ApiResponse.success(recs, "Recommendations generated");
    }

    @GetMapping("/pending")
    public ApiResponse<List<Recommendation>> pending() {
        return ApiResponse.success(recommendationService.listPending(), "Pending recommendations");
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<String> approve(@PathVariable String id) {
        boolean ok = recommendationService.approve(id);
        return ApiResponse.success(ok ? "Approved" : "Failed", ok ? "Recommendation approved" : "Approval failed");
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<String> reject(@PathVariable String id) {
        boolean ok = recommendationService.reject(id);
        return ApiResponse.success(ok ? "Rejected" : "Failed", ok ? "Recommendation rejected" : "Rejection failed");
    }

    @PostMapping("/{id}/execute")
    public ApiResponse<String> execute(@PathVariable String id) {
        boolean ok = recommendationService.execute(id);
        return ApiResponse.success(ok ? "Executed" : "Failed", ok ? "Recommendation executed" : "Execution failed");
    }

    @GetMapping("/history")
    public ApiResponse<List<Recommendation>> history() {
        return ApiResponse.success(recommendationService.history(), "Recommendation history");
    }
}
