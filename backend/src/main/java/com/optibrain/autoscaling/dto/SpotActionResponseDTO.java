package com.optibrain.autoscaling.dto;

import com.optibrain.autoscaling.model.ActionType;
import com.optibrain.autoscaling.model.SpotStatus;
import java.time.Instant;

public record SpotActionResponseDTO(
    ActionType type,
    String resourceId,
    SpotStatus status,
    double predictedSavings,
    Instant createdAt
) {}
