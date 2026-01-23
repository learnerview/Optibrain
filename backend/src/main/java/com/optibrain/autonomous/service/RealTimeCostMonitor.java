package com.optibrain.autonomous.service;

import com.optibrain.analytics.dto.AnomalyDTO;
import com.optibrain.analytics.dto.CostSummaryDTO;
import com.optibrain.analytics.service.AnalyticsService;
import com.optibrain.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RealTimeCostMonitor {

    private final AnalyticsService analyticsService;

    /**
     * Periodic cost health check (simulating real-time monitoring).
     * In a production environment, this would process a stream of billing metrics.
     */
    @Scheduled(fixedRateString = "${app.monitor.rate:300000}") // Every 5 minutes
    public void monitorCloudCosts() {
        // Since this runs in a background thread, we need to handle tenant context manually
        // In this demo, we'll monitor the "demo-tenant"
        TenantContext.setTenantId("demo-tenant");
        
        try {
            log.info("[MONITOR] Initiating autonomous cost health check for demo-tenant");
            
            CostSummaryDTO summary = analyticsService.getCostSummary();
            log.info("[MONITOR] Current Total Saved: ${}", summary.getTotalSaved());
            
            AnomalyDTO anomalies = analyticsService.getAnomalies();
            if (anomalies.getCount() > 0) {
                log.warn("[MONITOR] Detected {} potential cost anomalies. Triggering AI analysis...", anomalies.getCount());
                anomalies.getAnomalies().stream()
                        .filter(a -> "HIGH".equalsIgnoreCase(a.getSeverity()))
                        .forEach(a -> log.error("[ALERT] Critical Anomaly detected on {}: ${} (Deviation: {})", 
                                a.getDate(), a.getCost(), a.getDeviation()));
            } else {
                log.info("[MONITOR] Cost trajectory is stable. No anomalies detected.");
            }
            
            // Future logic: Trigger auto-scaling or cleanup if thresholds are breached
            
        } catch (Exception e) {
            log.error("[MONITOR] Failed to execute cost monitor: {}", e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
