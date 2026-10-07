package com.optibrain.cloud.model;

import java.time.Instant;

/**
 * Outcome of a {@link ResourceAction}.
 *
 * <p>A dry-run and a real run produce the same shape, differing only in
 * {@link #applied()}. That lets the UI and audit trail render "would have saved $X"
 * with exactly the same code path that renders a completed remediation.
 *
 * @param success whether the action completed (or, for dry-run, would have completed)
 * @param applied false for dry-runs and for actions rejected before dispatch
 * @param dryRun true when the action was evaluated but deliberately not applied
 * @param action the action that was attempted
 * @param resourceId the affected resource
 * @param message human-readable outcome
 * @param estimatedMonthlySavings projected savings from applying this action
 * @param error failure detail when {@code success} is false
 * @param executedAt when the action was evaluated
 */
public record ActionResult(
        boolean success,
        boolean applied,
        boolean dryRun,
        ActionType action,
        String resourceId,
        String message,
        Double estimatedMonthlySavings,
        String error,
        Instant executedAt
) {

    public static ActionResult simulated(ResourceAction request, String message, Double savings) {
        return new ActionResult(true, false, true, request.type(), request.resourceId(),
                message, savings, null, Instant.now());
    }

    public static ActionResult succeeded(ResourceAction request, String message, Double savings) {
        return new ActionResult(true, true, request.dryRun(), request.type(), request.resourceId(),
                message, savings, null, Instant.now());
    }

    public static ActionResult rejected(ResourceAction request, String reason) {
        return new ActionResult(false, false, request.dryRun(), request.type(), request.resourceId(),
                reason, null, reason, Instant.now());
    }

    public static ActionResult failed(ResourceAction request, Throwable cause) {
        return new ActionResult(false, false, request.dryRun(), request.type(), request.resourceId(),
                "Action failed", null, cause.getMessage(), Instant.now());
    }
}