package com.optibrain.autonomous.repository;

import com.optibrain.autonomous.model.OptimizationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OptimizationResultRepository extends JpaRepository<OptimizationResult, UUID> {
    List<OptimizationResult> findByTenantId(String tenantId);
}
