package com.optibrain.cloud.model;

/**
 * The operations OptiBrain can perform against a provider.
 *
 * <p>Risk is a property of the operation, not of the caller, so the policy engine and
 * the UI can both reason about blast radius from a single definition.
 */
public enum ActionType {

    /** Stop a running instance. Reversible. */
    STOP_INSTANCE(RiskLevel.LOW),
    /** Start a stopped instance. Reversible. */
    START_INSTANCE(RiskLevel.LOW),
    /** Change instance size/type. Requires a stop unless the provider supports live resize. */
    RESIZE_INSTANCE(RiskLevel.MEDIUM),
    /** Move a workload onto Spot or a Savings Plan commitment. */
    PURCHASE_COMMITMENT(RiskLevel.MEDIUM),
    /** Release an unassociated Elastic IP. Reversible. */
    RELEASE_ELASTIC_IP(RiskLevel.LOW),
    /** Delete a snapshot. Irreversible data loss. */
    DELETE_SNAPSHOT(RiskLevel.HIGH),
    /** Delete a volume. Irreversible data loss. */
    DELETE_VOLUME(RiskLevel.HIGH),
    /** Delete a bucket. Irreversible data loss. */
    DELETE_BUCKET(RiskLevel.HIGH),
    /** Delete a load balancer or NAT gateway. Service interruption until traffic moves. */
    DELETE_NETWORK_RESOURCE(RiskLevel.HIGH),
    /** Delete a database, cache, stream or queue. Irreversible. */
    DELETE_DATASTORE(RiskLevel.CRITICAL),
    /** Terminate an instance. Irreversible. */
    TERMINATE_INSTANCE(RiskLevel.CRITICAL),
    /** Scale an ASG or node group. Reversible. */
    SCALE_GROUP(RiskLevel.LOW),
    /** Apply or change a tag. Reversible. */
    APPLY_TAGS(RiskLevel.LOW),
    /** No-op used to test connectivity and permissions. */
    NOOP(RiskLevel.NONE);

    private final RiskLevel risk;

    ActionType(RiskLevel risk) {
        this.risk = risk;
    }

    public RiskLevel risk() {
        return risk;
    }

    public boolean isDestructive() {
        return risk.ordinal() >= RiskLevel.HIGH.ordinal();
    }

    /** Coarse blast-radius classification used for approval routing. */
    public enum RiskLevel {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}