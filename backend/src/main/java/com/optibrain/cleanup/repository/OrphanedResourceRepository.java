package com.optibrain.cleanup.repository;

import com.optibrain.cleanup.model.OrphanedResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrphanedResourceRepository extends JpaRepository<OrphanedResource, UUID> {
    List<OrphanedResource> findByTenantId(String tenantId);
    List<OrphanedResource> findByTenantIdAndResolved(String tenantId, boolean resolved);
}
