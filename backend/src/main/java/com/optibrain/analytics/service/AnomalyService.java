package com.optibrain.analytics.service;

import java.util.Map;

/**
 * Interface for Cost Anomaly Detection operations.
 * Contract frozen for Hackathon stabilization.
 */
public interface AnomalyService {
    Map<String, Object> getAnomalies();
}
