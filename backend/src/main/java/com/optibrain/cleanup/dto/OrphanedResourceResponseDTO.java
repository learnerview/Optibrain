package com.optibrain.cleanup.dto;

import java.time.Instant;

public record OrphanedResourceResponseDTO(
    String resourceId,
    String resourceType,
    String region,
    double estimatedMonthlyCost,
    boolean resolved,
    Instant createdAt
) {}
