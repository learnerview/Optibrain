package com.optibrain.metrics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricData {
    private String name;
    private double value;
    private String unit;
    private Instant timestamp;
    private Map<String, String> dimensions;
    
    // Legacy fields for compatibility
    private double cpuUtilization;
    private double memoryUtilization;
    private double hourlyCost;
    private int instanceCount;
    private LocalDateTime localTimestamp;
    private List<MetricSnapshot> historicalData;
    private Map<String, Double> serviceBreakdown;
    private String region;
}
