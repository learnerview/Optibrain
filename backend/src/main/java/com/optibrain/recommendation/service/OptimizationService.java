package com.optibrain.recommendation.service;

import java.util.List;
import java.util.Map;

/**
 * Interface for AI Recommendation/Optimization operations.
 * Contract frozen for Hackathon stabilization.
 */
public interface OptimizationService {
    List<Map<String, Object>> getRecommendations();

    /**
     * Regenerates pending recommendations from the current measured account state.
     * {@link #getRecommendations()} only reads the last generated list, so callers that
     * want a current view must call this first.
     */
    List<Map<String, Object>> refresh();
}
