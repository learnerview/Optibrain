package com.optibrain.autoscaling.service;

import com.optibrain.audit.service.AuditService;
import com.optibrain.chat.service.PyBridgeService;
import com.optibrain.cloud.config.CloudConfig;
import com.optibrain.decision.model.Decision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Predictive Scaling Service
 * Uses ML forecasts to enable pre-scaling before load spikes occur.
 * This connects the ML forecasting service to autoscaling decisions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PredictiveScalingService {

    private final PyBridgeService mlBridge;
    private final AutoscalingService autoscalingService;
    private final AuditService auditService;
    private final CloudConfig cloudConfig;
    private final com.optibrain.config.service.TenantConfigurationService configService;
    
    // Cache for recent forecasts
    private final Map<String, Map<String, Object>> forecastCache = new HashMap<>();
    private static final int FORECAST_CACHE_MINUTES = 5;
    
    // Lead time for predictive scaling (how many hours ahead to look)
    private static final int PREDICTIVE_LEAD_TIME_HOURS = 2;

    /**
     * Evaluate autoscaling decision with forecast data
     * Enables pre-scaling based on predicted load
     */
    public Decision evaluateWithForecast(String tenantId, String resourceId, 
                                         List<Double> historicalCpu, 
                                         List<Double> historicalMemory) {
        log.info("[PREDICTIVE] Evaluating scaling decision with forecast for tenant: {}", tenantId);
        
        var config = configService.getConfiguration(tenantId);
        
        // Get ML forecast
        Map<String, Object> cpuForecast = getForecast(tenantId, "cpu", historicalCpu);
        Map<String, Object> memoryForecast = getForecast(tenantId, "memory", historicalMemory);
        
        // Analyze forecasts for potential scaling actions
        String predictedAction = analyzeForecastForScaling(cpuForecast, memoryForecast, config);
        
        // Build decision based on forecast
        Decision decision = buildPredictiveDecision(tenantId, resourceId, predictedAction, 
                                                     cpuForecast, memoryForecast);
        
        // Add forecast data to decision
        Map<String, Object> forecastData = new HashMap<>();
        forecastData.put("cpuForecast", cpuForecast);
        forecastData.put("memoryForecast", memoryForecast);
        forecastData.put("forecastUsed", true);
        forecastData.put("leadTimeHours", PREDICTIVE_LEAD_TIME_HOURS);
        decision.setParameters(forecastData);
        
        log.info("[PREDICTIVE] Forecast-based decision: {} for resource {}", 
                decision.getAction(), resourceId);
        
        return decision;
    }
    
    /**
     * Get forecast from ML service with caching
     */
    public Map<String, Object> getForecast(String tenantId, String metricType, List<Double> history) {
        String cacheKey = tenantId + "_" + metricType;
        
        // Check cache
        if (forecastCache.containsKey(cacheKey)) {
            Map<String, Object> cached = forecastCache.get(cacheKey);
            LocalDateTime cachedAt = (LocalDateTime) cached.get("cachedAt");
            if (cachedAt != null && cachedAt.plusMinutes(FORECAST_CACHE_MINUTES).isAfter(LocalDateTime.now())) {
                log.debug("[PREDICTIVE] Using cached forecast for {}", cacheKey);
                return cached;
            }
        }
        
        // Get fresh forecast from ML service
        try {
            var config = configService.getConfiguration(tenantId);
            // Pass configuration mode if supported by bridge/ML service
            Map<String, Object> forecast = mlBridge.getForecastForScaling(tenantId, metricType, history);
            
            if (forecast != null && !forecast.isEmpty()) {
                // Create mutable copy of forecast to add cache metadata
                Map<String, Object> mutableForecast = new HashMap<>(forecast);
                mutableForecast.put("cachedAt", LocalDateTime.now());
                forecastCache.put(cacheKey, mutableForecast);
                log.info("[PREDICTIVE] Retrieved forecast for {}/{}: {} predictions", 
                        tenantId, metricType, getListSize(mutableForecast.get("predictions")));
                return mutableForecast;
            }
        } catch (Exception e) {
            log.error("[PREDICTIVE] Error getting forecast: {}", e.getMessage());
        }
        
        // Return empty forecast
        return Collections.emptyMap();
    }
    
    /**
     * Analyze forecasts to determine if predictive scaling is needed
     */
    private String analyzeForecastForScaling(Map<String, Object> cpuForecast, 
                                              Map<String, Object> memoryForecast,
                                              com.optibrain.config.model.TenantConfiguration config) {
        if (cpuForecast.isEmpty() && memoryForecast.isEmpty()) {
            log.debug("[PREDICTIVE] No forecast data available, returning NONE");
            return "NONE";
        }
        
        // Get dynamic thresholds
        double scaleUpThreshold = config.getCpuUpperBound();
        double scaleDownThreshold = config.getCpuLowerBound();
        
        // Get predictions for the lead time window
        List<Double> cpuPredictions = extractPredictions(cpuForecast, PREDICTIVE_LEAD_TIME_HOURS);
        List<Double> memoryPredictions = extractPredictions(memoryForecast, PREDICTIVE_LEAD_TIME_HOURS);
        
        // Calculate max predicted values in the lead time window
        double maxPredictedCpu = cpuPredictions.isEmpty() ? 50.0 : 
                cpuPredictions.stream().mapToDouble(d -> d).max().orElse(50.0);
        double maxPredictedMemory = memoryPredictions.isEmpty() ? 50.0 : 
                memoryPredictions.stream().mapToDouble(d -> d).max().orElse(50.0);
        
        // Calculate average predicted values
        double avgPredictedCpu = cpuPredictions.isEmpty() ? 50.0 : 
                cpuPredictions.stream().mapToDouble(d -> d).average().orElse(50.0);
        double avgPredictedMemory = memoryPredictions.isEmpty() ? 50.0 : 
                memoryPredictions.stream().mapToDouble(d -> d).average().orElse(50.0);
        
        log.info("[PREDICTIVE] Forecast analysis - CPU: max={}, avg={}; Memory: max={}, avg={}",
                maxPredictedCpu, avgPredictedCpu, maxPredictedMemory, avgPredictedMemory);
        
        // Determine action based on predictions and dynamic thresholds
        // SCALE_UP if predicted max exceeds upper bound
        if (maxPredictedCpu > scaleUpThreshold || maxPredictedMemory > scaleUpThreshold) {
            log.info("[PREDICTIVE] Predicted high load - recommending SCALE_UP");
            return "SCALE_UP";
        }
        
        // SCALE_DOWN if predicted average is below lower bound
        if (avgPredictedCpu < scaleDownThreshold && avgPredictedMemory < scaleDownThreshold) {
            log.info("[PREDICTIVE] Predicted low load - recommending SCALE_DOWN");
            return "SCALE_DOWN";
        }
        
        return "NONE";
    }
    
    /**
     * Build a decision based on predictive analysis
     */
    private Decision buildPredictiveDecision(String tenantId, String resourceId, 
                                              String action, Map<String, Object> cpuForecast,
                                              Map<String, Object> memoryForecast) {
        Decision decision = new Decision();
        decision.setDecisionId("predictive-" + UUID.randomUUID().toString().substring(0, 8));
        decision.setAction(action);
        decision.setResourceId(resourceId);
        decision.setGeneratedAt(LocalDateTime.now());
        decision.setMode("PREDICTIVE");
        
        // Calculate confidence based on forecast confidence
        double confidence = calculateAggregateConfidence(cpuForecast, memoryForecast);
        decision.setScore(confidence * 100);
        
        // Build explanation
        List<String> explanations = new ArrayList<>();
        explanations.add("Decision based on ML forecast for next " + PREDICTIVE_LEAD_TIME_HOURS + " hours");
        
        if (cpuForecast.containsKey("seasonality")) {
            explanations.add("Seasonality patterns detected and applied");
        }
        
        if ("SCALE_UP".equals(action)) {
            explanations.add("Predicted load spike - pre-scaling recommended");
        } else if ("SCALE_DOWN".equals(action)) {
            explanations.add("Predicted low utilization - scale-down recommended");
        }
        
        decision.setExplanation(explanations);
        decision.setReason("Predictive scaling based on ML forecast");
        
        return decision;
    }
    
    /**
     * Calculate aggregate confidence from multiple forecasts
     */
    private double calculateAggregateConfidence(Map<String, Object> cpuForecast, 
                                                 Map<String, Object> memoryForecast) {
        double cpuConfidence = extractAverageConfidence(cpuForecast);
        double memoryConfidence = extractAverageConfidence(memoryForecast);
        
        // Weight CPU slightly higher as it's often more predictive
        return (cpuConfidence * 0.6) + (memoryConfidence * 0.4);
    }
    
    @SuppressWarnings("unchecked")
    private double extractAverageConfidence(Map<String, Object> forecast) {
        if (forecast == null || !forecast.containsKey("confidence_scores")) {
            return 0.5;
        }
        
        Object scores = forecast.get("confidence_scores");
        if (scores instanceof List) {
            List<Number> confidenceList = (List<Number>) scores;
            return confidenceList.stream()
                    .mapToDouble(Number::doubleValue)
                    .average()
                    .orElse(0.5);
        }
        
        return 0.5;
    }
    
    @SuppressWarnings("unchecked")
    private List<Double> extractPredictions(Map<String, Object> forecast, int hours) {
        if (forecast == null || !forecast.containsKey("predictions")) {
            return Collections.emptyList();
        }
        
        Object predictions = forecast.get("predictions");
        if (predictions instanceof List) {
            List<Number> predList = (List<Number>) predictions;
            return predList.stream()
                    .limit(hours)
                    .map(Number::doubleValue)
                    .toList();
        }
        
        return Collections.emptyList();
    }
    
    private int getListSize(Object obj) {
        if (obj instanceof List) {
            return ((List<?>) obj).size();
        }
        return 0;
    }
    
    /**
     * Scheduled task to run predictive scaling analysis
     * Runs every 15 minutes to check for upcoming load changes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void runPredictiveAnalysis() {
        if (cloudConfig.isDryRun()) {
            log.debug("[PREDICTIVE] Skipping scheduled analysis in dry-run mode");
            return;
        }
        
        log.info("[PREDICTIVE] Running scheduled predictive scaling analysis");
        // In production, iterate through active tenants and resources
        // For now, this is a placeholder for the scheduled task
    }
    
    /**
     * Check if predictive scaling should be triggered
     */
    public boolean shouldTriggerPredictiveScaling(String tenantId, String resourceId,
                                                   List<Double> historicalCpu) {
        Map<String, Object> forecast = getForecast(tenantId, "cpu", historicalCpu);
        
        if (forecast.isEmpty()) {
            return false;
        }
        
        var config = configService.getConfiguration(tenantId);
        
        List<Double> predictions = extractPredictions(forecast, PREDICTIVE_LEAD_TIME_HOURS);
        double maxPredicted = predictions.stream()
                .mapToDouble(d -> d)
                .max()
                .orElse(50.0);
        
        return maxPredicted > config.getCpuUpperBound();
    }
}
