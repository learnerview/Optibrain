package com.optibrain.cloud.remediation.model;

/**
 * Outcome of a remediated operation, as persisted after each execution.
 *
 * <p>{@code SIMULATED} covers dry-runs and {@code REJECTED} covers actions refused before
 * dispatch (a stale plan, a protected resource, a block raised by the planner), so the
 * audit trail distinguishes "we decided not to" from "it failed".
 */
public enum RemediationStatus {
    SIMULATED,
    SUCCESS,
    FAILED,
    REJECTED
}