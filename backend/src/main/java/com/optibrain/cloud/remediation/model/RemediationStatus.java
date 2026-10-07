package com.optibrain.cloud.remediation.model;

/**
 * Outcome of a remediated operation, as persisted after each execution.
 *
 * <p>{@code SIMULATED} covers dry-runs and {@code REJECTED} covers actions refused before
 * dispatch (a stale plan, a protected resource, a block raised by the planner), so the
 * audit trail distinguishes "we decided not to" from "it failed".
 *
 * <p>{@code IN_PROGRESS} is a transient claim: it is written <strong>before</strong> an
 * idempotent action is dispatched so a unique constraint on the key can arbitrate
 * concurrent requests, and is replaced with the real outcome as soon as the action
 * settles. A row stuck at {@code IN_PROGRESS} means the process died mid-execution.
 */
public enum RemediationStatus {
    IN_PROGRESS,
    SIMULATED,
    SUCCESS,
    FAILED,
    REJECTED
}