package com.optibrain.cloud.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class AzureCloudAdapter implements CloudAdapter {
    
    @Override
    public String getProviderName() { return "AZURE"; }

    @Override
    public List<String> discoverInstances() {
        return List.of("azure-standard-d2-v2", "azure-standard-d4-v2");
    }

    @Override
    public boolean scaleUp(String resourceId) {
        log.info("[AZURE] Scaling up {}", resourceId);
        return true;
    }

    @Override
    public boolean scaleDown(String resourceId) {
        log.info("[AZURE] Scaling down {}", resourceId);
        return true;
    }

    @Override
    public boolean terminateResource(String resourceId) {
        log.warn("[AZURE] Terminating resource: {}", resourceId);
        return true;
    }
    @Override
    public double getCostEstimate(String resourceId) {
        return 180.00;
    }

    @Override
    public String getResourceType(String resourceId) {
        return "Standard_D2";
    }

    @Override
    public java.util.Map<String, String> getCurrentSpecs(String resourceId) {
        return java.util.Map.of("vCPU", "2", "Memory", "7GB");
    }
}
