package com.optibrain.tenant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * A tenant account.
 *
 * <p>This was a plain POJO built in memory by {@code TenantCredentialService}, which
 * threw {@code UnsupportedOperationException} on any profile other than {@code dev}. It
 * is now a persisted entity so tenants survive a restart and can be managed through the
 * API.
 *
 * <p>Deliberately does not extend {@code BaseEntity}: that class is for rows owned by a
 * tenant and carries a {@code tenantId} column, which would be circular here. The id is
 * the caller's tenant identifier, assigned on creation.
 *
 * <p>Credentials are <strong>not</strong> modelled here. Storing long-lived AWS keys
 * alongside a tenant row is the wrong shape; see {@code AwsClientFactory} for how
 * credentials are actually resolved. What is modelled is the IAM role {@code AwsClientFactory}
 * should {@code AssumeRole} into for this tenant (and the external id needed to
 * authenticate the assumption), so per-tenant scoping never requires a key column.
 */
@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenants_active", columnList = "active"),
        // Listing and lookup are by name on the admin screen and in selectors. Without
        // this the table scan is unavoidable, which matters once a deployment holds
        // thousands of tenants.
        @Index(name = "idx_tenants_name", columnList = "name")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @Column(nullable = false, length = 64)
    private String id;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /** The only supported provider. Retained for forward compatibility and for display. */
    @Builder.Default
    @Column(nullable = false, length = 32)
    private String cloudProvider = "AWS";

    /** MANUAL or AUTONOMOUS. AUTONOMOUS requires per-tenant approval to be enabled. */
    @Builder.Default
    @Column(nullable = false, length = 32)
    private String mode = "MANUAL";

    @Column(length = 32)
    private String plan;

    @Builder.Default
    @Column(nullable = false)
    private boolean autonomousMode = false;

    /**
     * Whether a remediation needs human approval. Defaults to true: the safest state,
     * and the one a new tenant should be in until it is deliberately changed.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean requireApprovalForChanges = true;

    @Builder.Default
    @Column(nullable = false)
    private double monthlyBudgetLimit = 0.0;

    /**
     * Resource ids or tag keys this tenant has marked as protected. Stored as a
     * comma-separated list rather than a join table because it is small, read on every
     * remediation decision, and never queried by element.
     */
    @Column(length = 2000)
    private String protectedResourcesCsv;

    /**
     * Tag keys every resource in this tenant must carry. Stored as a comma-separated
     * list because it is small and never queried by element. Null or blank means the
     * platform defaults ({@code Environment}, {@code Owner}).
     */
    @Column(length = 2000)
    private String requiredTagKeysCsv;

    /**
     * IAM role to assume for this tenant's AWS operations. In {@code cloud.mode=AWS},
     * a tenant with no role is refused (fail closed) rather than silently using the
     * OptiBrain instance's ambient credentials, unless
     * {@code cloud.aws.allow-ambient-fallback=true} opts an operator into that for a
     * single-account deployment. The sandbox ignores this entirely.
     */
    @Column(length = 2048)
    private String awsRoleArn;

    /**
     * External id required to assume {@link #getAwsRoleArn()}, if the role's trust
     * policy demands one. Optional; ignored when null or blank.
     */
    @Column(length = 1224)
    private String awsExternalId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** Last time this tenant performed an operation. Null until first use. */
    private Instant lastActivityAt;

    /**
     * Optimistic locking. Two operators editing the same tenant concurrently must not
     * silently overwrite each other; the second save fails instead.
     */
    @Version
    private Long version;

    public List<String> getProtectedResources() {
        if (protectedResourcesCsv == null || protectedResourcesCsv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(protectedResourcesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void setProtectedResources(List<String> protectedResources) {
        this.protectedResourcesCsv = protectedResources == null || protectedResources.isEmpty()
                ? null
                : String.join(",", protectedResources);
    }

    public List<String> getRequiredTagKeys() {
        if (requiredTagKeysCsv == null || requiredTagKeysCsv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(requiredTagKeysCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void setRequiredTagKeys(List<String> requiredTagKeys) {
        this.requiredTagKeysCsv = requiredTagKeys == null || requiredTagKeys.isEmpty()
                ? null
                : String.join(",", requiredTagKeys);
    }
}