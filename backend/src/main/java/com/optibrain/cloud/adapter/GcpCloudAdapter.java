package com.optibrain.cloud.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class GcpCloudAdapter implements CloudAdapter {
    
    @Override
    public String getProviderName() { return "GCP"; }

    @Override
    public List<String> discoverInstances() {
        return List.of("gcp-n2-standard-1", "gcp-n2-standard-2");
    }

    @Override
    public boolean scaleUp(String resourceId) {
        log.info("[GCP] Scaling up {}", resourceId);
        return true;
    }

    @Override
    public boolean scaleDown(String resourceId) {
        log.info("[GCP] Scaling down {}", resourceId);
        return true;
    }

    @Override
    public boolean terminateResource(String resourceId) {
        log.warn("[GCP] Terminating resource: {}", resourceId);
        return true;
    }
    @Override
    public double getCostEstimate(String resourceId) {
        return 160.00;
    }

    @Override
    public String getResourceType(String resourceId) {
        return "n2-standard-2";
    }

    @Override
    public java.util.Map<String, String> getCurrentSpecs(String resourceId) {
        return java.util.Map.of("vCPU", "2", "Memory", "8GB");
    }
}
