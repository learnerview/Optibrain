package com.optibrain.cloud.remediation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.optibrain.cloud.model.ActionType;

import java.util.List;

/**
 * The remediation plan for a single action.
 *
 * <p>Separated from {@link com.optibrain.cloud.model.ActionResult} because planning and
 * executing are different operations with different guarantees. A plan describes what
 * <em>would</em> happen and never touches the account, which is what makes it safe to
 * show in a UI before anyone approves anything.
 *
 * @param actionType the operation
 * @param resourceId the target
 * @param resourceType canonical type of the target, when it exists
 * @param region region of the target
 * @param currentState lifecycle state at planning time
 * @param protectedResource whether the resource carries the protection tag
 * @param risk blast radius of the operation
 * @param destructive whether the action removes data or interrupts a workload
 * @param monthlyCost attributed monthly cost of the target, or null when unattributed
 * @param estimatedMonthlySavings saving attributable to the action
 * @param blocked whether the protection tag forbids this action
 * @param blockedReason why, when blocked
 * @param dryRunOnly whether the global dry-run interlock is preventing application
 * @param sandboxed whether the target is a LocalStack sandbox rather than a real account
 * @param steps what the action would do, in order
 * @param token digest of the reactive inputs, so a caller can prove an execution is not
 *               operating on a plan the resource state no longer matches
 */
public record RemediationPlan(
        ActionType actionType,
        String resourceId,
        String resourceType,
        String region,
        String currentState,
        boolean protectedResource,
        ActionType.RiskLevel risk,
        boolean destructive,
        Double monthlyCost,
        Double estimatedMonthlySavings,
        boolean blocked,
        String blockedReason,
        boolean dryRunOnly,
        boolean sandboxed,
        List<String> steps,
        String token
) {

    public RemediationPlan {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    /**
     * Whether the action could be applied right now.
     *
     * <p>Derived from {@code blocked} and {@code dryRunOnly} so a client cannot read an
     * inconsistent pair. Exposed explicitly because records do not serialise derived
     * accessors, and having the server state the verdict keeps the UI from reimplementing
     * the rule.
     */
    @JsonProperty("executable")
    public boolean executable() {
        return !blocked && !dryRunOnly;
    }
}