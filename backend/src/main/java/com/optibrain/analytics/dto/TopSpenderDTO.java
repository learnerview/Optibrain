package com.optibrain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopSpenderDTO {
    private String resourceName;
    private String service;
    private double cost;
    private double percentage;
}
