package com.optibrain.recommendation.repository;

import com.optibrain.recommendation.model.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Persists recommendations per tenant so approval and execution state survives a
 * restart and the next regeneration.
 */
@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, String> {

    List<Recommendation> findByTenantId(String tenantId);

    List<Recommendation> findByTenantIdAndStatus(String tenantId, String status);

    Optional<Recommendation> findByIdAndTenantId(String id, String tenantId);

    void deleteByTenantIdAndStatus(String tenantId, String status);
}