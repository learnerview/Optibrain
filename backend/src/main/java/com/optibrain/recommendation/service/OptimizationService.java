package com.optibrain.recommendation.service;

import java.util.List;
import java.util.Map;

/**
 * Interface for AI Recommendation/Optimization operations.
 * Contract frozen for Hackathon stabilization.
 */
public interface OptimizationService {
    List<Map<String, Object>> getRecommendations();
}
