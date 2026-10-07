package com.optibrain.cloud.remediation;

import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.policy.ProtectionPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plans and performs remediation.
 *
 * <p>Planning and execution are deliberately separate calls. {@link #plan} performs no
 * mutation and returns everything a reviewer needs to decide, including whether policy
 * blocks the action. {@link #execute} applies it.
 *
 * <p>Execution passes the caller's {@code dryRun} flag to the provider, which also
 * applies the global interlock. Nothing here can force a change through: the provider
 * holds both guards and this service owns neither.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RemediationService {

    private final CloudProviderPort cloudProvider;
    private final CloudProperties properties;

    /** Bounded audit of every executed action so a reviewer can trace what changed. */
    private final java.util.List<ExecutionRecord> audit =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    private static final int AUDIT_CAPACITY = 500;

    /** Actions that have been requested this session, most recent first. */
    public java.util.List<ExecutionRecord> history() {
        synchronized (audit) {
            java.util.List<ExecutionRecord> snapshot = java.util.List.copyOf(audit);
            java.util.List<ExecutionRecord> reversed = new java.util.ArrayList<>(snapshot);
            java.util.Collections.reverse(reversed);
            return java.util.List.copyOf(reversed);
        }
    }

    /** The actions this deployment can plan or execute, with their blast radius. */
    public java.util.List<ActionSummary> availableActions() {
        return java.util.Arrays.stream(ActionType.values())
                .map(t -> new ActionSummary(t, t.risk(), t.isDestructive()))
                .toList();
    }

    /** One recorded execution. */
    public record ExecutionRecord(ActionType type, String resourceId, boolean applied,
                                  boolean dryRun, boolean success, String message,
                                  java.time.Instant executedAt) {}

    /** One action's supported surface. */
    public record ActionSummary(ActionType type, ActionType.RiskLevel risk, boolean destructive) {}

    /** Describes what an action would do, without touching the account. */
    public RemediationPlan plan(ActionType type, String resourceId, Map<String, String> parameters) {
        Optional<CloudResource> target = cloudProvider.find(resourceId);

        boolean protectedResource = target.map(CloudResource::protectedResource).orElse(false);
        boolean destructive = type.isDestructive();
        boolean unknownTarget = target.isEmpty() && destructive;

        // Blocked means either policy refuses it (protection tag) or the guard could
        // not verify the target. Reporting "not blocked" on an unverifiable destructive
        // action would tell a reviewer the change was safe when it could not be checked.
        boolean blocked = (protectedResource && destructive) || unknownTarget;
        String blockedReason = blocked
                ? (protectedResource
                        ? "Resource is protected by the " + ProtectionPolicy.TAG + " tag"
                        : "Target resource could not be found in the inventory; "
                                + "action blocked until the guard can verify it")
                : null;

        return new RemediationPlan(
                type,
                resourceId,
                target.map(r -> r.type().typeName()).orElse("unknown"),
                target.map(CloudResource::region).orElse(cloudProvider.region()),
                target.map(CloudResource::state).orElse("not found"),
                protectedResource,
                type.risk(),
                destructive,
                target.map(CloudResource::monthlyCost).orElse(null),
                estimatedSaving(type, target),
                blocked,
                blockedReason,
                // The interlock is read from configuration, not inferred from the account
                // being a sandbox. A sandbox can run with dry-run disabled, and reporting
                // otherwise would tell a reviewer their change is blocked when it is not.
                properties.isDryRun(),
                cloudProvider.isSandboxed(),
                stepsFor(type, parameters));
    }

    /** Applies an action. Honours the global dry-run interlock inside the provider. */
    public ActionResult execute(ActionType type, String resourceId,
                                Map<String, String> parameters, boolean dryRun) {
        ResourceAction action = ResourceAction.of(type, resourceId,
                parameters == null ? Map.of() : parameters, dryRun, "api request");
        ActionResult result = cloudProvider.execute(action);
        audit.add(new ExecutionRecord(type, resourceId, result.applied(), result.dryRun(),
                result.success(), result.message(), java.time.Instant.now()));
        while (audit.size() > AUDIT_CAPACITY) {
            audit.remove(0);
        }
        return result;
    }

    /**
     * Saving is only stated where cost is attributed and the action actually removes
     * or pauses that cost.
     *
     * <p>Stopping, terminating, deleting or releasing a resource relinquishes its
     * billed cost; starting, scaling or tagging it does not, so those report no saving.
     * Any commitment adjustment belongs to the recommendation engine, which has access
     * to the effective rate, and presenting a list-price figure here would invite the
     * reader to treat it as net.
     */
    private Double estimatedSaving(ActionType type, Optional<CloudResource> target) {
        boolean removesCost = switch (type) {
            case STOP_INSTANCE, TERMINATE_INSTANCE, DELETE_VOLUME, DELETE_SNAPSHOT,
                    RELEASE_ELASTIC_IP -> true;
            default -> false;
        };
        if (target.isEmpty() || !removesCost) {
            return null;
        }
        Double monthly = target.get().monthlyCost();
        return monthly == null ? null : monthly;
    }

    private List<String> stepsFor(ActionType type, Map<String, String> parameters) {
        return switch (type) {
            case STOP_INSTANCE -> List.of("Stop the instance", "Billing continues until it terminates");
            case START_INSTANCE -> List.of("Start the instance");
            case TERMINATE_INSTANCE -> List.of(
                    "Terminate the instance",
                    "This deletes its root volume and cannot be undone");
            case DELETE_VOLUME -> List.of(
                    "Delete the volume",
                    "Data on the volume is destroyed and cannot be recovered");
            case DELETE_SNAPSHOT -> List.of(
                    "Delete the snapshot",
                    "Data captured by the snapshot is destroyed");
            case RELEASE_ELASTIC_IP -> List.of(
                    "Release the address",
                    "The address becomes available to anyone to claim");
            case APPLY_TAGS -> List.of("Apply tag "
                    + (parameters == null ? "<key>=<value>" : parameters.get("key") + "="
                    + parameters.get("value")));
            case SCALE_GROUP -> List.of("Set desired capacity to "
                    + (parameters == null ? "<value>" : parameters.get("desiredCapacity")));
            case RESIZE_INSTANCE -> List.of("Change instance type to "
                    + (parameters == null ? "<type>" : parameters.get("instanceType")));
            default -> List.of("No AWS implementation for " + type);
        };
    }
}