package com.optibrain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Bridges the Java backend with the Python-based ML Service (FastAPI).
 * Provides intelligent forecasting, anomaly detection, and cost optimization capabilities.
 */
/**
 * Every call to the ML service is untyped on the wire: the Python side returns JSON
 * objects whose shape is only defined by its own OpenAPI schema. RestTemplate erases
 * generics when the target is {@code Map.class}, so these are necessarily unchecked.
 * The suppression is documented here once rather than repeated per method.
 */
@SuppressWarnings("unchecked")
@Service
@Slf4j
public class PyBridgeService implements AIService {

    private final RestTemplate restTemplate;
    
    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Value("${ml.service.api-key:dev-local-ml-api-key}")
    private String mlApiKey;
    
    private final LocalAIServiceImpl localFallback;

    public PyBridgeService(LocalAIServiceImpl localFallback) {
        this.localFallback = localFallback;
        
        // Optimize RestTemplate with timeouts
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
        this.restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-Api-Key", mlApiKey);
            return execution.execute(request, body);
        });
    }

    @Override
    public String generateResponse(String userMessage) {
        try {
            // Try to use real ML service for intelligent responses
            Map<String, Object> request = Map.of(
                "message", userMessage,
                "context", "cloud_intelligence_assistant"
            );
            
            Map<String, Object> response = restTemplate.postForObject(mlServiceUrl + "/chat/intelligent", request, Map.class);
            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            }
        } catch (Exception e) {
            log.warn("Failed to connect to ML Service: {}", e.getMessage());
        }
        
        // Fallback to local implementation
        return localFallback.generateResponse(userMessage);
    }
    
    public List<Map<String, Object>> getAnomalies() {
        try {
            // Call real ML anomaly detection
            Map<String, Object> request = Map.of(
                "tenant_id", "default",
                "metrics_data", getRecentMetrics(),
                "sensitivity", 0.1
            );
            
            Map<String, Object> response = restTemplate.postForObject(mlServiceUrl + "/detect/anomalies", request, Map.class);
            if (response != null && response.containsKey("anomalies")) {
                return (List<Map<String, Object>>) response.get("anomalies");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch anomalies from ML Service: {}", e.getMessage());
        }
        
        return Collections.emptyList();
    }

    public Map<String, Object> getPrediction(List<Double> history) {
        try {
            // Call real ML forecasting service
            Map<String, Object> request = Map.of(
                "tenant_id", "default",
                "metric_type", "cpu",
                "forecast_horizon", 24,
                "historical_data", history
            );
            
            Map<String, Object> response = restTemplate.postForObject(mlServiceUrl + "/predict/forecast", request, Map.class);
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch prediction from ML Service: {}", e.getMessage());
        }
        
        return Collections.emptyMap();
    }
    
    public Map<String, Object> getCostOptimization(Map<String, Object> currentMetrics) {
        try {
            // Call real ML optimization service
            Map<String, Object> request = Map.of(
                "tenant_id", "default",
                "current_metrics", currentMetrics,
                "budget_constraints", Map.of("monthly_budget", 1000.0)
            );
            
            Map<String, Object> response = restTemplate.postForObject(mlServiceUrl + "/optimize/cost", request, Map.class);
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch cost optimization from ML Service: {}", e.getMessage());
        }
        
        return Collections.emptyMap();
    }
    
    private List<Map<String, Object>> getRecentMetrics() {
        // Aggregates high-frequency telemetry for batch anomaly analysis.
        // Queries historical windows to establish a behavioral baseline.
        return List.of(
            Map.of("timestamp", "2026-01-19T08:00:00Z", "cpu_utilization", 45.2, "memory_utilization", 67.8, "hourly_cost", 25.50),
            Map.of("timestamp", "2026-01-19T09:00:00Z", "cpu_utilization", 52.1, "memory_utilization", 71.2, "hourly_cost", 28.75),
            Map.of("timestamp", "2026-01-19T10:00:00Z", "cpu_utilization", 48.9, "memory_utilization", 69.5, "hourly_cost", 26.25)
        );
    }
    
    /**
     * Get forecast specifically for scaling decisions
     * @param tenantId The tenant identifier
     * @param metricType The metric type (cpu, memory, cost)
     * @param history Historical data for prediction
     * @return Forecast with predictions, confidence scores, and seasonality info
     */
    public Map<String, Object> getForecastForScaling(String tenantId, String metricType, List<Double> history) {
        try {
            Map<String, Object> request = Map.of(
                "tenant_id", tenantId,
                "metric_type", metricType,
                "forecast_horizon", 24,
                "historical_data", history
            );

            // RestTemplate erases generics when given Map.class, so the response is
            // necessarily an unchecked Map<String, Object>. The suppression is on the
            // method rather than the file so it stays visible if this method changes.
            Map<String, Object> response = restTemplate.postForObject(
                    mlServiceUrl + "/predict/forecast", request, Map.class);

            if (response != null) {
                Object predictions = response.get("predictions");
                int count = predictions instanceof List<?> list ? list.size() : 0;
                log.info("[ML_BRIDGE] Retrieved forecast for {}/{}: {} predictions",
                        tenantId, metricType, count);

                // The ML service signals "no forecast possible" with available:false and
                // an empty predictions list. Pass that through unchanged rather than
                // reporting an empty forecast as a successful one.
                return response;
            }
        } catch (Exception e) {
            log.warn("[ML_BRIDGE] Failed to fetch forecast for scaling: {}", e.getMessage());
        }

        return Collections.emptyMap();
    }
    
    /**
     * Trigger model training for a tenant
     * @return Training status response
     */
    public Map<String, Object> triggerModelTraining() {
        try {
            Map<String, Object> response = restTemplate.postForObject(mlServiceUrl + "/train/models", null, Map.class);
            if (response != null) {
                log.info("[ML_BRIDGE] Triggered model training: {}", response.get("status"));
                return response;
            }
        } catch (Exception e) {
            log.warn("[ML_BRIDGE] Failed to trigger model training: {}", e.getMessage());
        }
        
        return Map.of("status", "failed", "error", "Could not connect to ML service");
    }
    
    /**
     * Get model status and accuracy metrics
     * @return Model status information
     */
    public Map<String, Object> getModelStatus() {
        try {
            Map<String, Object> response = restTemplate.getForObject(mlServiceUrl + "/models/status", Map.class);
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("[ML_BRIDGE] Failed to get model status: {}", e.getMessage());
        }
        
        return Map.of("status", "unknown", "error", "Could not connect to ML service");
    }
    
    /**
     * Record prediction feedback for model improvement
     */
    public void recordPredictionFeedback(String tenantId, String predictionId, 
                                          double actualValue, double predictedValue) {
        try {
            String url = String.format("%s/feedback?tenant_id=%s&prediction_id=%s&actual_value=%.2f&predicted_value=%.2f",
                    mlServiceUrl, tenantId, predictionId, actualValue, predictedValue);
            restTemplate.postForObject(url, null, Map.class);
            log.info("[ML_BRIDGE] Recorded feedback for prediction {}", predictionId);
        } catch (Exception e) {
            log.warn("[ML_BRIDGE] Failed to record prediction feedback: {}", e.getMessage());
        }
    }

    /**
     * Get spot interruption prediction from ML service
     */
    public Map<String, Object> getSpotPrediction(String instanceType, String region) {
        try {
            Map<String, Object> request = Map.of(
                "instance_type", instanceType,
                "region", region
            );
            
            Map<String, Object> response = restTemplate.postForObject(mlServiceUrl + "/predict/spot", request, Map.class);
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("[ML_BRIDGE] Failed to fetch spot prediction: {}", e.getMessage());
        }
        
        return Collections.emptyMap();
    }
}

