package com.optibrain.autonomous.dto;

import java.time.Instant;

public record OptimizationResultResponseDTO(
    int recommendationsGenerated,
    int recommendationsExecuted,
    double estimatedMonthlySavings,
    boolean success,
    String errorMessage,
    Instant createdAt
) {}
