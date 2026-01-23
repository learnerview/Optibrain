package com.optibrain.cloud.adapter;

import java.util.List;

public interface CloudAdapter {
    String getProviderName();
    List<String> discoverInstances();
    boolean scaleUp(String resourceId);
    boolean scaleDown(String resourceId);
    boolean terminateResource(String resourceId);
    
    double getCostEstimate(String resourceId);
    String getResourceType(String resourceId);
    java.util.Map<String, String> getCurrentSpecs(String resourceId);
}
