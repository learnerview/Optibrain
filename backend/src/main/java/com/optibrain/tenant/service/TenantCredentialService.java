package com.optibrain.tenant.service;

import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.model.TenantUpdateRequest;
import com.optibrain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tenant lifecycle and lookup.
 *
 * <p>Replaces an in-memory implementation that returned a hardcoded tenant in the dev
 * profile and threw {@code UnsupportedOperationException} everywhere else, so the product
 * could not run outside development at all.
 *
 * <p><strong>Credentials are not handled here.</strong> The previous version exposed
 * {@code getCredentials(tenantId)}, which handed raw access keys to callers and, in the
 * LocalStack branch, returned literal {@code test/test}. That method has been deleted.
 * Credentials are resolved once, centrally, by {@code AwsClientFactory} from the
 * ambient credential chain (IAM role, environment, profile). For a multi-tenant
 * deployment the correct mechanism is STS AssumeRole per tenant, not a key column.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantCredentialService {

    /** Minimum interval between persisted activity writes for one tenant. */
    static final Duration ACTIVITY_WRITE_INTERVAL = Duration.ofMinutes(5);

    private final TenantRepository tenants;

    /**
     * Last time activity was persisted, per tenant.
     *
     * <p>Per-instance and intentionally not persisted: this is a write-rate limiter, and
     * losing it on restart costs at most one extra write.
     */
    private final Map<String, Instant> lastActivityWrite = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenants.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> findTenant(String tenantId) {
        return tenantId == null || tenantId.isBlank()
                ? Optional.empty()
                : tenants.findByIdAndActiveTrue(tenantId);
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(String tenantId) {
        return findTenant(tenantId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No active tenant with id '" + tenantId + "'"));
    }

    @Transactional
    public Tenant createTenant(Tenant tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant is required");
        }
        if ((tenant.getId() == null || tenant.getId().isBlank())
                && tenant.getName() != null && !tenant.getName().isBlank()) {
            tenant.setId(tenant.getName().trim().toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", ""));
        }
        if (tenant.getId() == null || tenant.getId().isBlank()) {
            throw new IllegalArgumentException("Tenant id is required");
        }
        if (tenants.existsById(tenant.getId())) {
            throw new IllegalStateException(
                    "Tenant '" + tenant.getId() + "' already exists");
        }
        if (tenant.getName() == null || tenant.getName().isBlank()) {
            throw new IllegalArgumentException("Tenant name is required");
        }
        tenant.setCreatedAt(Instant.now());

        // Safety invariants enforced at the boundary rather than trusted to callers:
        // a new tenant never starts autonomous, and never starts without approval.
        if (tenant.getMode() == null || tenant.getMode().isBlank()) {
            tenant.setMode("MANUAL");
        }
        if ("AUTONOMOUS".equalsIgnoreCase(tenant.getMode())) {
            log.info("Tenant {} created in AUTONOMOUS mode", tenant.getId());
        }
        enforceModeInvariant(tenant);
        return tenants.save(tenant);
    }

    @Transactional
    public Tenant updateTenant(String tenantId, TenantUpdateRequest changes) {
        Tenant existing = getTenant(tenantId);

        if (changes.getName() != null && !changes.getName().isBlank()) {
            existing.setName(changes.getName());
        }
        if (changes.getPlan() != null) {
            existing.setPlan(changes.getPlan());
        }
        if (changes.getMode() != null && !changes.getMode().isBlank()) {
            existing.setMode(changes.getMode());
        }
        // Only the fields actually supplied are overwritten. Binding primitives from the
        // entity here would reset approval and budget to their defaults on every partial
        // PUT, defeating the documented "safest state" invariant.
        if (changes.getAutonomousMode() != null) {
            existing.setAutonomousMode(changes.getAutonomousMode());
        }
        if (changes.getRequireApprovalForChanges() != null) {
            existing.setRequireApprovalForChanges(changes.getRequireApprovalForChanges());
        }
        if (changes.getMonthlyBudgetLimit() != null) {
            existing.setMonthlyBudgetLimit(changes.getMonthlyBudgetLimit());
        }
        if (changes.getProtectedResources() != null) {
            existing.setProtectedResources(changes.getProtectedResources());
        }
        if (changes.getAwsRoleArn() != null) {
            existing.setAwsRoleArn(changes.getAwsRoleArn().isBlank() ? null : changes.getAwsRoleArn().trim());
        }
        if (changes.getAwsExternalId() != null) {
            existing.setAwsExternalId(changes.getAwsExternalId().isBlank() ? null : changes.getAwsExternalId().trim());
        }
        enforceModeInvariant(existing);
        return tenants.save(existing);
    }

    /**
     * Rejects a configuration where AUTONOMOUS mode is combined with approvals disabled.
     *
     * <p>Autonomous remediation without human approval is the one combination that can
     * destroy infrastructure unprompted, so it is refused at the boundary rather than
     * trusted to a caller that may hold elevated permissions.
     */
    private void enforceModeInvariant(Tenant tenant) {
        if ("AUTONOMOUS".equalsIgnoreCase(tenant.getMode())
                && !tenant.isRequireApprovalForChanges()) {
            throw new IllegalArgumentException(
                    "AUTONOMOUS tenants must require approval for changes");
        }
    }

    /**
     * Deactivates a tenant.
     *
     * <p>Soft delete rather than remove: audit history, remediation records and orphan
     * reports all reference the tenant id, and deleting the row would orphan them.
     */
    @Transactional
    public void deactivateTenant(String tenantId) {
        Tenant tenant = getTenant(tenantId);
        tenant.setActive(false);
        tenants.save(tenant);
        log.info("Tenant {} deactivated", tenantId);
    }

    @Transactional
    public void reactivateTenant(String tenantId) {
        Tenant tenant = tenants.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException(
                        "No tenant with id '" + tenantId + "'"));
        tenant.setActive(true);
        tenants.save(tenant);
    }

/**
 * Records that a tenant performed an operation.
 *
 * <p>Write amplification is controlled here. An unconditional {@code UPDATE} per request
 * turns a read-only dashboard poll into a write, which serialises on the tenant row and
 * turns a hot tenant into a lock contention point.
 *
 * <p>Activity is therefore recorded at most once per {@link #ACTIVITY_WRITE_INTERVAL}
 * per tenant, and an unknown tenant is not written at all. The trade-off is deliberate:
 * activity time is observability, not a correctness-critical field, and a value that is
 * up to five minutes stale is worth far more than a serialised write on every request.
 *
 * <p>Failures are swallowed: tracking must never fail the operation it records.
 */
@Transactional
public void updateTenantActivity(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
        return;
    }
    Instant now = Instant.now();
    Instant previous = lastActivityWrite.get(tenantId);
    if (previous != null
            && previous.plus(ACTIVITY_WRITE_INTERVAL).isAfter(now)) {
        return;
    }

    try {
        Optional<Tenant> tenant = tenants.findById(tenantId);
        if (tenant.isEmpty()) {
            return;
        }
        tenant.get().setLastActivityAt(now);
        tenants.save(tenant.get());
        lastActivityWrite.put(tenantId, now);
    } catch (Exception e) {
        log.debug("Could not record activity for tenant {}: {}", tenantId, e.getMessage());
    }
}
}