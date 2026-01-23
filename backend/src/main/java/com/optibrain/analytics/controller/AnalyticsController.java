package com.optibrain.analytics.controller;

import com.optibrain.analytics.dto.FinancialReportResponseDTO;
import com.optibrain.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.optibrain.analytics.dto.AnomalyDTO;
import com.optibrain.analytics.dto.CostSummaryDTO;
import com.optibrain.analytics.dto.ForecastDTO;
import com.optibrain.common.dto.ApiResponse;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ApiResponse<CostSummaryDTO> getSummary() {
        return ApiResponse.success(analyticsService.getCostSummary());
    }

    @GetMapping("/forecast")
    public ApiResponse<ForecastDTO> getForecast() {
        return ApiResponse.success(analyticsService.getForecast());
    }

    @GetMapping("/anomalies")
    public ApiResponse<AnomalyDTO> getAnomalies() {
        return ApiResponse.success(analyticsService.getAnomalies());
    }

    @GetMapping("/report")
    public ApiResponse<FinancialReportResponseDTO> getReport(
            @RequestParam(defaultValue = "MONTHLY") String period) {
        return ApiResponse.success(analyticsService.generateReport(period));
    }
}
