package com.optibrain.cloud.remediator;

import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.remediation.RemediationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Entry point for remediation requests coming from the decision engine.
 *
 * <p>Every mutation flows through {@link RemediationService}, so a change requested from
 * here is planned against the live inventory, blocked when its plan is stale or refused
 * by policy, recorded in the audit trail, and subject to the same idempotency rules as a
 * change from the API. There is no second path that skips those guards.
 */
@Component
@Slf4j
public class CloudRemediator {

    private final RemediationService remediation;

    public CloudRemediator(RemediationService remediation) {
        this.remediation = remediation;
    }

    public ActionResult executeScaleUp(String region, String resourceId) {
        return execute(region, resourceId, ActionType.START_INSTANCE, Map.of());
    }

    public ActionResult executeScaleDown(String region, String resourceId) {
        return execute(region, resourceId, ActionType.STOP_INSTANCE, Map.of());
    }

    public ActionResult applyRightsizing(String instanceId, String newType) {
        return execute(null, instanceId, ActionType.RESIZE_INSTANCE,
                Map.of("instanceType", newType));
    }

    private ActionResult execute(String region, String resourceId, ActionType type,
                                 Map<String, String> parameters) {
        log.info("Remediation {} requested for {} in {}", type, resourceId, region);
        // Automated actions apply only when the environment permits it; the provider's
        // global dry-run interlock, reflected back through the plan, stops them cold when
        // it is enabled.
        return remediation.execute(type, resourceId, parameters, false);
    }
}