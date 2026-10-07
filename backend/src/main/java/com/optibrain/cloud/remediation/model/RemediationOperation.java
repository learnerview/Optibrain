package com.optibrain.cloud.remediation.model;

import com.optibrain.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A persisted record of one executed remediation operation.
 *
 * <p>Persisting the outcome gives an operation an identity independent of any HTTP
 * request, which is what makes retries safe: the {@code idempotencyKey} column is a
 * client-supplied correlation id scoped to the tenant, and replaying a request with the
 * same key returns the stored outcome instead of executing the mutation twice.
 *
 * <p>The {@code (tenantId, idempotencyKey)} pair is <strong>unique</strong>. Uniqueness
 * is enforced by the database, not by a check-then-insert in the service, so two
 * concurrent requests carrying the same key cannot both pass the check: the loser's
 * claim insert fails on the constraint and it replays the winner's stored outcome. See
 * {@code RemediationService} for the claim-before-dispatch flow.
 */
@Entity
@Table(name = "remediation_operations", indexes = {
        @Index(name = "idx_rem_ops_tenant_idem", columnList = "tenantId, idempotencyKey", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RemediationOperation extends BaseEntity {

    /** Client-supplied correlation id; null for ad-hoc calls without one. */
    @Column(length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 128)
    private String resourceId;

    @Column(nullable = false)
    private boolean dryRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RemediationStatus status;

    @Column(length = 2000)
    private String message;

    private Double savings;
}