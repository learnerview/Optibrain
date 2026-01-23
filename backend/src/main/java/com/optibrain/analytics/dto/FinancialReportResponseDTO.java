package com.optibrain.analytics.dto;

import java.time.Instant;
import java.util.List;

public record FinancialReportResponseDTO(
    String period,
    double currentCost,
    double previousCost,
    double forecastedCost,
    double potentialSavings,
    List<TopSpenderDTO> topSpenders,
    String source,
    Instant createdAt
) {}
