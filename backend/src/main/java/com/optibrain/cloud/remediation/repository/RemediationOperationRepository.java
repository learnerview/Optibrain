package com.optibrain.cloud.remediation.repository;

import com.optibrain.cloud.remediation.model.RemediationOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link RemediationOperation}.
 *
 * <p>Lookup is scoped to the tenant so a caller can only replay its own operations: the
 * idempotency key is unique per tenant, not globally.
 */
@Repository
public interface RemediationOperationRepository extends JpaRepository<RemediationOperation, UUID> {

    Optional<RemediationOperation> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);
}