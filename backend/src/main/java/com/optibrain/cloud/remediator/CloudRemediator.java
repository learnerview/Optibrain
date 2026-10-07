package com.optibrain.cloud.remediator;

import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.port.CloudProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Entry point for remediation requests coming from the API layer.
 *
 * <p>Dry-run enforcement lives in {@link CloudProviderPort#execute} so that it cannot be
 * bypassed by a caller; this class only translates a legacy request shape into a
 * {@link ResourceAction} and hands it over.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CloudRemediator {

    private final CloudProviderPort cloudProvider;
    private final com.optibrain.cloud.config.CloudProperties cloudProperties;

    public ActionResult executeScaleUp(String region, String resourceId) {
        return execute(region, resourceId, ActionType.START_INSTANCE, Map.of());
    }

    public ActionResult executeScaleDown(String region, String resourceId) {
        return execute(region, resourceId, ActionType.STOP_INSTANCE, Map.of());
    }

    public ActionResult applyRightsizing(String instanceId, String newType) {
        return execute(null, instanceId, ActionType.RESIZE_INSTANCE, Map.of("instanceType", newType));
    }

    private ActionResult execute(String region, String resourceId, ActionType type, Map<String, String> parameters) {
        ResourceAction action = ResourceAction.of(type, resourceId, parameters, cloudProperties.isDryRun(),
                "remediation requested via " + type);
        log.info("Remediation {} requested for {} in {}", type, resourceId, region);
        return cloudProvider.execute(action);
    }
}