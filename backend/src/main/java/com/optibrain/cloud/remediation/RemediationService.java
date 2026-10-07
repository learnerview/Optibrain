package com.optibrain.cloud.remediation;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.model.AuditStatus;
import com.optibrain.audit.service.AuditService;
import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.policy.ProtectionPolicy;
import com.optibrain.cloud.remediation.model.RemediationOperation;
import com.optibrain.cloud.remediation.model.RemediationStatus;
import com.optibrain.cloud.remediation.repository.RemediationOperationRepository;
import com.optibrain.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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
 *
 * <p>Every execution is the single path through which a mutation reaches the account,
 * and every execution is recorded: a {@link RemediationOperation} row gives an operation
 * an identity (surviving retries via its idempotency key), and an audit row joins the
 * optimization history. Before dispatch the plan is re-derived from the resource's live
 * state, so a plan that no longer matches reality is refused instead of executed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RemediationService {

    private final CloudProviderPort cloudProvider;
    private final CloudProperties properties;
    private final AuditService auditService;
    private final RemediationOperationRepository operations;

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

        String state = target.map(CloudResource::state).orElse("not found");
        String region = target.map(CloudResource::region).orElse(cloudProvider.region());
        String resourceType = target.map(r -> r.type().typeName()).orElse("unknown");
        boolean dryRunOnly = properties.isDryRun();
        boolean sandboxed = cloudProvider.isSandboxed();

        RemediationPlan plan = new RemediationPlan(
                type,
                resourceId,
                resourceType,
                region,
                state,
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
                dryRunOnly,
                sandboxed,
                stepsFor(type, parameters),
                planToken(type, resourceId, state, protectedResource, dryRunOnly, sandboxed,
                        parameters));
        return plan;
    }

    /**
     * Applies an action. Honours the global dry-run interlock inside the provider.
     */
    public ActionResult execute(ActionType type, String resourceId,
                                Map<String, String> parameters, boolean dryRun) {
        return execute(type, resourceId, parameters, dryRun, null, null);
    }

    /**
     * Applies an action with idempotency and staleness control.
     *
     * @param idempotencyKey client-supplied correlation id; resubmitting the same key
     *                       replays the stored outcome instead of mutating again
     * @param planToken token from the plan a reviewer approved, if provided; a token that
     *                  no longer matches the resource's live state refuses the execution
     */
    public ActionResult execute(ActionType type, String resourceId,
                                Map<String, String> parameters, boolean dryRun,
                                String idempotencyKey, String planToken) {
        Map<String, String> safeParameters = parameters == null ? Map.of() : parameters;

        String tenantId = TenantContext.getTenantId();
        RemediationOperation replayed = replayIfKnown(tenantId, idempotencyKey);
        if (replayed != null) {
            log.info("Replayed {} on {} from idempotency key {}", type, resourceId,
                    idempotencyKey);
            return replay(replayed, type, resourceId, safeParameters);
        }

        // The plan is re-derived from the live inventory rather than taken on trust: the
        // resource may have been terminated, protected or restarted since the reviewer
        // saw the plan, and executing a stale plan would apply a change to that reality.
        RemediationPlan plan = plan(type, resourceId, safeParameters);
        ResourceAction request = ResourceAction.of(type, resourceId, safeParameters, dryRun,
                "api request", idempotencyKey);

        if (planToken != null && !planToken.isBlank() && !planToken.equals(plan.token())) {
            ActionResult result = ActionResult.rejected(request,
                    "The plan is stale: the resource state no longer matches it. "
                            + "Recompute the plan and retry.");
            record(request, result, null);
            return result;
        }
        if (plan.blocked()) {
            ActionResult result = ActionResult.rejected(request, plan.blockedReason());
            record(request, result, null);
            return result;
        }
        if (!dryRun && plan.dryRunOnly()) {
            ActionResult result = ActionResult.rejected(request,
                    "The global dry-run interlock is enabled; no change can be applied");
            record(request, result, null);
            return result;
        }

        // Claim the idempotency key before dispatch. The unique (tenantId, key)
        // constraint is what arbitrates concurrency: the request whose claim insert wins
        // runs, and every concurrent duplicate hits the violation and replays that
        // outcome. Reserving before dispatch also means a persistence failure here
        // surfaces before any mutation happens, not after.
        IdempotencyClaim claim = claimIdempotency(tenantId, request);
        if (claim.contended()) {
            RemediationOperation latest = replayIfKnown(tenantId, idempotencyKey);
            if (latest != null) {
                log.info("Idempotency key {} was already reserved; replaying its outcome",
                        idempotencyKey);
                return replay(latest, type, resourceId, safeParameters);
            }
            return ActionResult.rejected(request,
                    "Another request is already executing idempotency key " + idempotencyKey
                            + "; its outcome is not stored yet");
        }

        ActionResult result;
        try {
            result = cloudProvider.execute(request);
        } catch (RuntimeException e) {
            // The provider normally converts failures into a FAILED result; an exception
            // escaping is still a failed action and must be recorded as one.
            log.warn("Dispatch failed for {} on {}: {}", type, resourceId, e.getMessage());
            result = ActionResult.failed(request, e);
        }

        record(request, result, claim.row());
        return result;
    }

    private void record(ResourceAction request, ActionResult result, RemediationOperation claim) {
        audit.add(new ExecutionRecord(request.type(), request.resourceId(), result.applied(),
                result.dryRun(), result.success(), result.message(), Instant.now()));
        while (audit.size() > AUDIT_CAPACITY) {
            audit.remove(0);
        }
        // A failure to persist either the operation or its audit row must surface. An
        // action that cannot be recorded cannot honestly be reported as success; the old
        // catch-and-continue is exactly what made persistence failures invisible.
        persistOutcome(request, result, claim);
        persistAudit(request, result);
    }

    /**
     * Reserves an idempotency key as an {@code IN_PROGRESS} operation row.
     *
     * <p>Returns the claimed row when this request won the claim, or a contended marker
     * when the unique {@code (tenantId, idempotencyKey)} constraint refused the insert -
     * meaning a concurrent request already holds the key and will write the durable
     * outcome. A request without a key skips the claim entirely.
     */
    private IdempotencyClaim claimIdempotency(String tenantId, ResourceAction request) {
        String key = request.idempotencyKey();
        if (key == null || key.isBlank() || tenantId == null || tenantId.isBlank()) {
            return new IdempotencyClaim(null, false);
        }
        RemediationOperation claim = RemediationOperation.builder()
                .idempotencyKey(key)
                .action(request.type().name())
                .resourceId(request.resourceId())
                .dryRun(request.dryRun())
                .status(RemediationStatus.IN_PROGRESS)
                .message("Execution started")
                .build();
        claim.setTenantId(tenantId);
        try {
            operations.saveAndFlush(claim);
            log.info("Reserved idempotency key {} for {} on {}", key, request.type(),
                    request.resourceId());
            return new IdempotencyClaim(claim, false);
        } catch (DataIntegrityViolationException e) {
            log.info("Idempotency key {} already reserved; a concurrent request holds it", key);
            return new IdempotencyClaim(claim, true);
        }
    }

    private record IdempotencyClaim(RemediationOperation row, boolean contended) {
    }

    /**
     * Persists the settled outcome onto the claimed row, or a new row when no key was
     * claimed. Throws on failure so a broken database can never masquerade as a
     * successful remediation.
     */
    private void persistOutcome(ResourceAction request, ActionResult result,
                                RemediationOperation claim) {
        RemediationStatus status = statusOf(result);
        String message = result.message() != null ? result.message() : result.error();
        Double savings = result.estimatedMonthlySavings();
        if (claim != null) {
            claim.setStatus(status);
            claim.setMessage(message);
            claim.setSavings(savings);
            claim.setDryRun(request.dryRun());
            operations.save(claim);
            return;
        }
        RemediationOperation operation = RemediationOperation.builder()
                .idempotencyKey(request.idempotencyKey())
                .action(request.type().name())
                .resourceId(request.resourceId())
                .dryRun(request.dryRun())
                .status(status)
                .message(message)
                .savings(savings)
                .build();
        operation.setTenantId(TenantContext.getTenantId());
        operations.save(operation);
    }

    private void persistAudit(ResourceAction request, ActionResult result) {
        if (auditService == null) {
            return;
        }
        AuditLog entry = new AuditLog();
        entry.setDecisionId("rem-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        entry.setMode("REMEDIATION");
        entry.setRegion(cloudProvider.region());
        entry.setReason("remediation via " + request.reason());
        entry.setProviderSource("remediation");
        entry.setAction(request.type().name());
        entry.setResourceId(request.resourceId());
        entry.setStatus(result.success() ? AuditStatus.SUCCESS : AuditStatus.FAILED);
        entry.setExplanation(truncate(result.message() != null ? result.message() : result.error(), 1000));
        entry.setSavings(result.estimatedMonthlySavings() == null ? 0.0
                : result.estimatedMonthlySavings());
        entry.setScore(0.0);
        auditService.record(entry);
    }

    /** Returns a stored operation for the tenant and key, or null when it is new. */
    private RemediationOperation replayIfKnown(String tenantId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || tenantId == null || tenantId.isBlank() || operations == null) {
            return null;
        }
        return operations.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey).orElse(null);
    }

    /** Reconstructs the stored outcome instead of mutating the account a second time. */
    private ActionResult replay(RemediationOperation existing, ActionType type,
                                String resourceId, Map<String, String> parameters) {
        ResourceAction request = ResourceAction.of(type, resourceId, parameters,
                existing.isDryRun(), "replayed from idempotency key",
                existing.getIdempotencyKey());
        String message = existing.getMessage() == null ? "Replayed from a previous execution"
                : existing.getMessage();
        return switch (existing.getStatus()) {
            case SUCCESS -> ActionResult.succeeded(request, message, existing.getSavings());
            case SIMULATED -> ActionResult.simulated(request, message, existing.getSavings());
            case IN_PROGRESS -> new ActionResult(false, false, existing.isDryRun(), type, resourceId,
                    "A previous execution with this idempotency key did not complete",
                    null, "Execution did not complete", Instant.now());
            case FAILED -> new ActionResult(false, false, existing.isDryRun(), type, resourceId,
                    message, existing.getSavings(),
                    existing.getMessage() == null ? "Action failed" : existing.getMessage(),
                    Instant.now());
            case REJECTED -> ActionResult.rejected(request, message);
        };
    }

    private RemediationStatus statusOf(ActionResult result) {
        if (result.success() && result.applied()) {
            return RemediationStatus.SUCCESS;
        }
        if (result.dryRun()) {
            return RemediationStatus.SIMULATED;
        }
        if (!result.success() && "Action failed".equals(result.message())) {
            return RemediationStatus.FAILED;
        }
        return RemediationStatus.REJECTED;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    /**
     * A short digest of the plan inputs that can change while a plan sits on a desk: the
     * resource's lifecycle state, its protection flag, the interlock and the parameters.
     * Two plans that a reviewer would reason about identically share a token, and any of
     * those inputs changing produces a different one.
     */
    private String planToken(ActionType type, String resourceId, String state,
                             boolean protectedResource, boolean dryRunOnly, boolean sandboxed,
                             Map<String, String> parameters) {
        StringBuilder base = new StringBuilder();
        base.append(type).append('|').append(resourceId).append('|').append(state).append('|')
                .append(protectedResource).append('|').append(dryRunOnly).append('|')
                .append(sandboxed);
        if (parameters != null) {
            parameters.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(e -> base.append('|').append(e.getKey()).append('=').append(e.getValue()));
        }
        String input = base.toString();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java platform, so this is unreachable in practice.
            // Degrading to String.hashCode() would let distinct plans collide and replay a
            // stale plan, so the process must stop rather than sign badly.
            throw new IllegalStateException("SHA-256 is unavailable, cannot sign plan tokens", e);
        }
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