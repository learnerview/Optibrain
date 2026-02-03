package com.optibrain.recommendation.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of OptimizationService
 */
@Service
public class OptimizationServiceImpl implements OptimizationService {
    
    @Override
    public List<Map<String, Object>> getRecommendations() {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        recommendations.add(createRecommendation(
            "1",
            "Resize EC2 Instance",
            "Instance i-abc123 is underutilized. Downsize to save 35%",
            "HIGH",
            450.50,
            "EC2"
        ));
        
        recommendations.add(createRecommendation(
            "2",
            "Switch to Reserved Instances",
            "Convert 5 on-demand instances to save 40%",
            "HIGH",
            1200.00,
            "EC2"
        ));
        
        recommendations.add(createRecommendation(
            "3",
            "Delete Unused EBS Volumes",
            "3 unattached EBS volumes detected",
            "MEDIUM",
            120.30,
            "EBS"
        ));
        
        recommendations.add(createRecommendation(
            "4",
            "Optimize S3 Storage Class",
            "Move infrequently accessed objects to S3 IA",
            "MEDIUM",
            85.75,
            "S3"
        ));
        
        return recommendations;
    }
    
    private Map<String, Object> createRecommendation(String id, String title, String description, String priority, double estimatedSavings, String service) {
        Map<String, Object> recommendation = new HashMap<>();
        recommendation.put("id", id);
        recommendation.put("title", title);
        recommendation.put("description", description);
        recommendation.put("priority", priority);
        recommendation.put("estimatedSavings", estimatedSavings);
        recommendation.put("service", service);
        recommendation.put("status", "PENDING");
        return recommendation;
    }
}
