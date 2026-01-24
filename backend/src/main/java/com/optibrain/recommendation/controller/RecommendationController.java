package com.optibrain.recommendation.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.recommendation.service.OptimizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class RecommendationController {

    private final OptimizationService optimizationService;

    @GetMapping("/recommendations")
    public ApiResponse<List<Map<String, Object>>> getRecommendations() {
        return ApiResponse.success(optimizationService.getRecommendations(), "Recommendations retrieved");
    }
}
