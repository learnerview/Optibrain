package com.optibrain.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSnapshot {
    private String tenantId;
    private Instant timestamp;
    private List<MetricData> metrics;
    private double totalCost;
    
    // Legacy fields for compatibility
    private LocalDateTime localTimestamp;
    private double cpu;
    private double memory;
}
