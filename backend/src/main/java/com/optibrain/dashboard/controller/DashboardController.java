package com.optibrain.dashboard.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.dashboard.service.DashboardOverviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("mainDashboardController")
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class DashboardController {
    
    private final DashboardOverviewService overviewService;
    
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getDashboardOverview() {
        try {
            Map<String, Object> overview = overviewService.getOverview();
            return ApiResponse.success(overview, "Dashboard overview retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting dashboard overview: {}", e.getMessage());
            return ApiResponse.error("Failed to get dashboard overview: " + e.getMessage());
        }
    }
}
