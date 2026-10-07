package com.optibrain.audit.repository;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.model.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByResourceId(String resourceId, Pageable pageable);
    List<AuditLog> findByStatus(AuditStatus status);
    List<AuditLog> findByTenantId(String tenantId);
}
