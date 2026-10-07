package com.optibrain.tenant.repository;

import com.optibrain.tenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for {@link Tenant}.
 *
 * <p>Tenants are stored in the {@code tenants} table, so they survive a restart and are
 * reachable through {@code /api/tenants}.
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {

    List<Tenant> findByActiveTrueOrderByNameAsc();

    Optional<Tenant> findByIdAndActiveTrue(String id);

    boolean existsById(String id);
}