package com.optibrain.cleanup.dto;

import java.time.Instant;

public record OrphanedResourceResponseDTO(
    String resourceId,
    String resourceType,
    String region,
    Double estimatedMonthlyCost,
    boolean resolved,
    Instant createdAt
) {}
