package com.optibrain.cloud.port;

import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ResourceAction;

/**
 * Write access to a provider.
 *
 * <p>This is the only path to a mutation in the entire system. Policy checks, dry-run
 * handling, audit recording and provider dispatch are layered around it, so no service
 * can bypass governance by calling an SDK client directly - which is exactly how the
 * previous design produced a scheduler able to delete real resources unattended.
 */
public interface RemediationExecutor {

    /**
     * Apply an action.
     *
     * <p>Implementations must honour {@link ResourceAction#dryRun()} and must never
     * throw for an expected failure - return an {@link ActionResult} with
     * {@code success=false} so the caller gets a uniform result shape.
     */
    ActionResult execute(ResourceAction action);
}