package com.optibrain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Data
@Slf4j
public class DynamicConfig {
    
    private final Map<String, Object> dynamicProperties = new ConcurrentHashMap<>();
    
    @Value("${app.dashboard.refresh-interval:30}")
    private long dashboardRefreshInterval;
    
    @Value("${app.ml.prediction-horizon:24}")
    private int predictionHorizon;
    
    @Value("${app.alerts.enabled:true}")
    private boolean alertsEnabled;
    
    @Value("${app.recommendations.auto-execute:false}")
    private boolean autoExecuteRecommendations;
    
    public Object getProperty(String key) {
        return dynamicProperties.get(key);
    }
    
    public void setProperty(String key, Object value) {
        dynamicProperties.put(key, value);
        log.info("Dynamic property updated: {} = {}", key, value);
    }
    
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        Object value = dynamicProperties.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public String getStringProperty(String key, String defaultValue) {
        Object value = dynamicProperties.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    public int getIntProperty(String key, int defaultValue) {
        Object value = dynamicProperties.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}
