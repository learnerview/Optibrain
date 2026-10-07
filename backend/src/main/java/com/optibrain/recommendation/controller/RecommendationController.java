package com.optibrain.recommendation.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/recommendations")
    public ApiResponse<List<Map<String, Object>>> getRecommendations() {
        return ApiResponse.success(recommendationService.refresh(),
                "Recommendations retrieved");
    }

    /**
     * Approves a pending recommendation so it becomes eligible for execution.
     * Requires CLOUD_INTELLIGENCE_ANALYST or higher.
     */
    @PostMapping("/recommendations/{id}/approve")
    public ApiResponse<Boolean> approve(@PathVariable String id) {
        boolean approved = recommendationService.approve(id);
        return ApiResponse.success(approved,
                approved ? "Recommendation approved" : "Recommendation is not pending");
    }

    /**
     * Rejects a pending recommendation.
     * Requires CLOUD_INTELLIGENCE_ANALYST or higher.
     */
    @PostMapping("/recommendations/{id}/reject")
    public ApiResponse<Boolean> reject(@PathVariable String id) {
        boolean rejected = recommendationService.reject(id);
        return ApiResponse.success(rejected,
                rejected ? "Recommendation rejected" : "Recommendation is not pending");
    }

    /**
     * Executes an approved recommendation.
     *
     * <p>Execution flows through the remediation pipeline, so the live-inventory check,
     * protection guard, global dry-run interlock and audit recording apply exactly as
     * they do for a manual change. A {@code false} response means the change was blocked
     * by one of those guards, not that it was performed.
     *
     * @return {@code true} when the recommendation was applied or simulated successfully
     */
    @PostMapping("/recommendations/{id}/execute")
    public ApiResponse<Boolean> execute(@PathVariable String id) {
        boolean executed = recommendationService.execute(id);
        return ApiResponse.success(executed, executed
                ? "Recommendation executed"
                : "Recommendation was blocked or is not approved");
    }
}