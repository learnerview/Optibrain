package com.optibrain.cloud.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Mock Cloud Adapter for Testing and Development
 * 
 * PURPOSE: This adapter provides fake cloud resources for:
 * - Unit testing
 * - Integration testing without real cloud infrastructure
 * - UI development without AWS costs
 * 
 * USAGE:
 * - Set cloud.mode=MOCK in application properties
 * - OR configure tenant with mode="mock"
 * 
 * PRODUCTION WARNING:
 * This adapter should NEVER be used in production.
 * It returns hardcoded fake data and does not interact with real cloud infrastructure.
 * All operations are no-ops that only log the action.
 * 
 * For production, use:
 * - AwsCloudAdapter (for real AWS)
 * - LocalstackCloudAdapter (for local AWS testing with LocalStack)
 */
@Component
@Profile({"dev", "test"})  // Only available in dev/test profiles
@Slf4j
public class MockCloudAdapter implements CloudAdapter {

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public List<String> discoverInstances() {
        log.debug("[MOCK] Discovering instances - returning fake data");
        return List.of("i-mock-001", "i-mock-002", "i-mock-003");
    }

    @Override
    public boolean scaleUp(String resourceId) {
        log.info("[MOCK] Scaling up resource: {} (no actual action taken)", resourceId);
        return true;
    }

    @Override
    public boolean scaleDown(String resourceId) {
        log.info("[MOCK] Scaling down resource: {} (no actual action taken)", resourceId);
        return true;
    }

    @Override
    public boolean terminateResource(String resourceId) {
        log.warn("[MOCK] Terminating resource: {} (no actual action taken)", resourceId);
        return true;
    }

    @Override
    public double getCostEstimate(String resourceId) {
        log.debug("[MOCK] Getting cost estimate for resource: {} - returning fake data", resourceId);
        return 25.50;
    }

    @Override
    public String getResourceType(String resourceId) {
        log.debug("[MOCK] Getting resource type for: {} - returning fake data", resourceId);
        return "t3.medium";
    }

    @Override
    public Map<String, String> getCurrentSpecs(String resourceId) {
        log.debug("[MOCK] Getting current specs for resource: {} - returning fake data", resourceId);
        return Map.of(
            "instance_type", "t3.medium",
            "vcpu", "2",
            "memory", "4GB",
            "storage", "100GB"
        );
    }
}
