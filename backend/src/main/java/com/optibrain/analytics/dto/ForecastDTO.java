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
public class ForecastDTO {
    private String period;
    private int daysAhead;
    private List<ForecastPointDTO> forecast;
    private String confidence;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ForecastPointDTO {
        private String date;
        private double predictedCost;
    }
}
