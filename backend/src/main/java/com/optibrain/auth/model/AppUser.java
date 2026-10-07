package com.optibrain.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * A persisted application user.
 *
 * <p>Users are backed by the database rather than an in-memory map so that accounts
 * survive a restart, registration is durable, and a multi-instance deployment shares one
 * identity store.
 *
 * <p>Tenant membership is a single {@code tenantId} column: the tenant a user belongs
 * to. {@code TenantFilter} derives the {@code TenantContext} from this membership and
 * never from a caller-supplied header, so the client cannot select another tenant by
 * sending {@code X-TENANT}.
 */
@Entity
@Table(name = "app_users", indexes = {
        @Index(name = "idx_app_users_username", columnList = "username", unique = true),
        @Index(name = "idx_app_users_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    /** Granted authorities as a comma-separated list, e.g. {@code ROLE_USER,ROLE_ADMIN}. */
    @Column(nullable = false, length = 255)
    private String authoritiesCsv;

    /** The tenant this user belongs to. Never derived from a request header. */
    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(length = 128)
    private String company;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public List<String> getRoles() {
        if (authoritiesCsv == null || authoritiesCsv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(authoritiesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}