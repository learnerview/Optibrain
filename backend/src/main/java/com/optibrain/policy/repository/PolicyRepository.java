package com.optibrain.policy.repository;

import com.optibrain.policy.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for {@link Policy}.
 *
 * <p>Every lookup and mutation is scoped to the tenant so no service can read or change
 * another tenant's policies. The id alone is not enough: it is generated, so it carries
 * no tenant information.
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, String> {

    Optional<Policy> findFirstByTenantIdAndEnabledTrueOrderByPriorityDesc(String tenantId);

    List<Policy> findByTenantIdOrderByPriorityDesc(String tenantId);

    Optional<Policy> findByIdAndTenantId(String id, String tenantId);

    void deleteByIdAndTenantId(String id, String tenantId);

    boolean existsByTenantId(String tenantId);
}