package com.optibrain.analytics.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.analytics.service.AnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Map;

/**
 * Controller for Cost Anomaly Detection.
 * Decoupled from concrete implementations for Hackathon stability.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AnomalyController {

    private final AnomalyService anomalyService;

    @GetMapping("/anomalies")
    public ApiResponse<Map<String, Object>> getAnomalies() {
        return ApiResponse.success(anomalyService.getAnomalies(), "Anomalies retrieved successfully");
    }
}
