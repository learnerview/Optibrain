package com.optibrain.cloud.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MockCloudAdapter implements CloudAdapter {

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public List<String> discoverInstances() {
        log.info("[MOCK] Discovering instances");
        return List.of("i-mock-001", "i-mock-002", "i-mock-003");
    }

    @Override
    public boolean scaleUp(String resourceId) {
        log.info("[MOCK] Scaling up resource: {}", resourceId);
        return true;
    }

    @Override
    public boolean scaleDown(String resourceId) {
        log.info("[MOCK] Scaling down resource: {}", resourceId);
        return true;
    }

    @Override
    public boolean terminateResource(String resourceId) {
        log.info("[MOCK] Terminating resource: {}", resourceId);
        return true;
    }

    @Override
    public double getCostEstimate(String resourceId) {
        log.info("[MOCK] Getting cost estimate for resource: {}", resourceId);
        return 25.50;
    }

    @Override
    public String getResourceType(String resourceId) {
        log.info("[MOCK] Getting resource type for: {}", resourceId);
        return "t3.medium";
    }

    @Override
    public Map<String, String> getCurrentSpecs(String resourceId) {
        log.info("[MOCK] Getting current specs for resource: {}", resourceId);
        return Map.of(
            "instance_type", "t3.medium",
            "vcpu", "2",
            "memory", "4GB",
            "storage", "100GB"
        );
    }
}
