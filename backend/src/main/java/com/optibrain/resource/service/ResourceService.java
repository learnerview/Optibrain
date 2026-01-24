package com.optibrain.resource.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Resource Inventory functionality
 */
public interface ResourceService {
    
    /**
     * Get all resources
     * @return List of resources with details
     */
    List<Map<String, Object>> getAllResources();
    
    /**
     * Get resource by ID
     * @param id Resource ID
     * @return Resource details
     */
    Map<String, Object> getResourceById(String id);
    
    /**
     * Get resources by status
     * @param status Resource status (Running, Idle, Orphaned)
     * @return List of resources
     */
    List<Map<String, Object>> getResourcesByStatus(String status);
}
