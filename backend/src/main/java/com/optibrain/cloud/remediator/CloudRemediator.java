package com.optibrain.cloud.remediator;

import com.optibrain.cloud.config.CloudConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CloudRemediator {

    private final com.optibrain.cloud.adapter.CloudAdapter cloudAdapter;
    private final CloudConfig cloudConfig;

    /**
     * Executes scale up action for a specific resource in a region
     * @param region The cloud region where the resource exists
     * @param resourceId The specific resource to scale up
     * @return true if scaling was successful
     */
    public boolean executeScaleUp(String region, String resourceId) {
        if (cloudConfig.isDryRun()) {
            log.info("[DRY-RUN] Would SCALE_UP resource {} in region: {}", resourceId, region);
            return true;
        }
        log.info("[EXECUTION] Sending SCALE_UP command for resource {} in region: {}", resourceId, region);
        return cloudAdapter.scaleUp(resourceId);
    }

    /**
     * Executes scale down action for a specific resource in a region
     * @param region The cloud region where the resource exists
     * @param resourceId The specific resource to scale down
     * @return true if scaling was successful
     */
    public boolean executeScaleDown(String region, String resourceId) {
        if (cloudConfig.isDryRun()) {
            log.info("[DRY-RUN] Would SCALE_DOWN resource {} in region: {}", resourceId, region);
            return true;
        }
        log.info("[EXECUTION] Sending SCALE_DOWN command for resource {} in region: {}", resourceId, region);
        return cloudAdapter.scaleDown(resourceId);
    }

    public boolean applyRightsizing(String instanceId, String newType) {
        if (cloudConfig.isDryRun()) {
            log.info("[DRY-RUN] Would rightsize instance {} to {}", instanceId, newType);
            return true;
        }
        log.info("[EXECUTION] Rightsizing instance {} to {} type", instanceId, newType);
        return true;
    }
}
