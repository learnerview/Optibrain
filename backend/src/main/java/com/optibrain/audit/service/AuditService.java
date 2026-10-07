package com.optibrain.audit.service;

import com.optibrain.audit.dto.AuditLogResponseDTO;
import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.model.AuditStatus;
import com.optibrain.audit.repository.AuditLogRepository;
import com.optibrain.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    public void record(AuditLog log) {
        log.setTenantId(TenantContext.getTenantId());
        repository.save(log);
    }

    public List<AuditLogResponseDTO> recent() {
        return recentEntities().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<AuditLog> recentEntities() {
        return repository.findByTenantId(TenantContext.getTenantId());
    }

    /**
     * A page of the current tenant's audit history.
     *
     * <p>The tenant is scoped to {@link TenantContext}, never to a caller-supplied
     * parameter, so one tenant can never read another's history.
     */
    public Page<AuditLogResponseDTO> getLogs(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        return repository.findByTenantId(tenantId, pageable).map(this::mapToDTO);
    }

    /**
     * Executions the current tenant applied in the last hour.
     *
     * <p>Counted from the persisted audit trail rather than an estimate, so the
     * "max changes per hour" guardrail reflects what actually ran.
     */
    public long countExecutionsInLastHour() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return 0;
        }
        return repository.countByTenantIdAndStatusAndCreatedAtAfter(
                tenantId, AuditStatus.SUCCESS, Instant.now().minus(Duration.ofHours(1)));
    }

    private AuditLogResponseDTO mapToDTO(AuditLog entity) {
        return new AuditLogResponseDTO(
            entity.getAction(),
            entity.getResourceId(),
            entity.getStatus().name(),
            entity.getExplanation(),
            entity.getSavings(),
            entity.getScore(),
            entity.getCreatedAt()
        );
    }
}
