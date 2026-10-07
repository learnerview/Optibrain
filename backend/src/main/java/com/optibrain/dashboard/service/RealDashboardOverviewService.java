package com.optibrain.dashboard.service;

import com.optibrain.roi.service.ROITrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The only {@link DashboardOverviewService}. There is no hardcoded demo variant: the
 * sandbox returns real numbers because it runs this same class against LocalStack.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealDashboardOverviewService implements DashboardOverviewService {

    private final ROITrackingService roiService;
    
    @Override
    public Map<String, Object> getOverview() {
        // Logic moved from DashboardController
        Map<String, Object> executiveSummary = roiService.getExecutiveSummary();
        Map<String, Object> realTimeMetrics = roiService.getRealTimeMetrics();
        List<Map<String, Object>> topPerformers = roiService.getTopPerformers();
        
        // Simplified for stability
        Map<String, Object> overview = new HashMap<>();
        overview.put("executiveSummary", executiveSummary);
        overview.put("realTimeMetrics", realTimeMetrics);
        overview.put("topPerformers", topPerformers);
        overview.put("lastUpdated", LocalDateTime.now());
        
        return overview;
    }
}
