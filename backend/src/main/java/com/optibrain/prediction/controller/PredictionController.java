package com.optibrain.prediction.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.chat.service.PyBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prediction")
@RequiredArgsConstructor
public class PredictionController {

    private final PyBridgeService pyBridgeService;

    @GetMapping("/forecast")
    public ApiResponse<Map<String, Object>> getForecast() {
        // Mock history for now, or fetch real metrics to pass to ML
        List<Double> history = List.of(100.0, 102.0, 98.0, 105.0, 110.0);
        Map<String, Object> prediction = pyBridgeService.getPrediction(history);
        
        // Transform to Frontend format (ForecastResponse)
        List<Map<String, Object>> forecastPoints = new ArrayList<>();
        if (prediction != null && prediction.containsKey("predictions")) {
            List<Double> preds = (List<Double>) prediction.get("predictions");
            for (int i = 0; i < preds.size(); i++) {
                Map<String, Object> point = new HashMap<>();
                point.put("date", java.time.LocalDate.now().plusDays(i + 1).toString());
                point.put("predictedCost", preds.get(i));
                forecastPoints.add(point);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("period", "30d");
        response.put("daysAhead", 30);
        response.put("forecast", forecastPoints);
        response.put("confidence", "HIGH");
        response.put("basis", "ML Ensemble Model");

        return ApiResponse.success(response);
    }

    @GetMapping("/anomalies")
    public ApiResponse<Map<String, Object>> getAnomalies() {
        List<Map<String, Object>> anomalies = pyBridgeService.getAnomalies();
        
        // Transform to Frontend format (AnomalyResponse)
        List<Map<String, Object>> mappedAnomalies = new ArrayList<>();
        if (anomalies != null) {
            for (Map<String, Object> a : anomalies) {
                Map<String, Object> mapped = new HashMap<>();
                mapped.put("date", java.time.LocalDate.now().toString()); 
                mapped.put("cost", 0.0);
                mapped.put("zScore", 3.5);
                mapped.put("severity", "HIGH");
                mapped.put("expectedCost", 0.0);
                mapped.put("deviation", 0.0);
                mappedAnomalies.add(mapped);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("count", mappedAnomalies.size());
        response.put("daysAnalyzed", 30);
        response.put("anomalies", mappedAnomalies);
        response.put("statistics", Map.of(
            "mean", 100.0,
            "stdDev", 10.0,
            "min", 80.0,
            "max", 150.0,
            "threshold", 130.0
        ));

        return ApiResponse.success(response);
    }
}
