package com.optibrain.prediction.service;

import com.optibrain.prediction.model.Forecast;
import com.optibrain.prediction.predictor.Predictor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final Predictor predictor;

    public Forecast generateForecast(List<Double> recentCpuHistory) {
        return predictor.predict(recentCpuHistory);
    }

    public Forecast generateForecast() {
        // Generate forecast with mock data
        List<Double> mockHistory = List.of(65.0, 68.0, 72.0, 70.0, 75.0);
        return predictor.predict(mockHistory);
    }
}
