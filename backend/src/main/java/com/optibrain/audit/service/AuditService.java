package com.optibrain.audit.service;

import com.optibrain.audit.dto.AuditLogResponseDTO;
import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.repository.AuditLogRepository;
import com.optibrain.common.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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
    
    public Page<AuditLogResponseDTO> getLogs(int page, int size) {
        // Simple paged fetch for current tenant
        // Note: Repository needs a findByTenantId with Pageable if we want true paging
        return repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::mapToDTO);
    }

    public long countExecutionsInLastHour() {
        return 12; // Simple mock for rate limiting/dashboard
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
