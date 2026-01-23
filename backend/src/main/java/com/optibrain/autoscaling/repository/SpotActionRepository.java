package com.optibrain.autoscaling.repository;

import com.optibrain.autoscaling.model.SpotAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpotActionRepository extends JpaRepository<SpotAction, UUID> {
    List<SpotAction> findByTenantId(String tenantId);
}
