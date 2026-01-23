package com.optibrain.analytics.controller;

import com.optibrain.analytics.dto.FinancialReportResponseDTO;
import com.optibrain.analytics.service.AnalyticsService;
import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics/reports")
@RequiredArgsConstructor
public class FinancialReportController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FinancialReportResponseDTO>>> getReports() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getReports()));
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<FinancialReportResponseDTO>> generateReport(
            @RequestParam(defaultValue = "MONTHLY") String period) {
        FinancialReportResponseDTO report = analyticsService.generateReport(period);
        return ResponseEntity.ok(ApiResponse.success(report));
    }
}
