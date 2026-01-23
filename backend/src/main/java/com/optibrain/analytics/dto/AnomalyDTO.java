package com.optibrain.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AnomalyDTO {
    private int count;
    private List<AnomalyItemDTO> anomalies;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AnomalyItemDTO {
        private String date;
        private double cost;
        private String severity; // LOW, MEDIUM, HIGH
        private double deviation;
    }
}
