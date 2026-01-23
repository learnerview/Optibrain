package com.optibrain.audit.dto;

import java.time.Instant;

public record AuditLogResponseDTO(
    String action,
    String resourceId,
    String status,
    String explanation,
    double savings,
    double score,
    Instant createdAt
) {
    public String getAction() { return action; }
    public Instant getTimestamp() { return createdAt; }
}
