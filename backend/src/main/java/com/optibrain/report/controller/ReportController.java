package com.optibrain.report.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/monthly")
    public ApiResponse<Map<String, Object>> getMonthlyReport() {
        try {
            Map<String, Object> report = reportService.getMonthlyReport();
            return ApiResponse.success(report, "Monthly report retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting monthly report: {}", e.getMessage());
            return ApiResponse.error("Failed to get monthly report: " + e.getMessage());
        }
    }
}
