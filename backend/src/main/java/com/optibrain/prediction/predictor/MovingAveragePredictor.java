package com.optibrain.prediction.predictor;

import com.optibrain.prediction.model.Forecast;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MovingAveragePredictor implements Predictor {

    @Override
    public Forecast predict(List<Double> history) {
        double sum = 0;
        for (Double val : history) {
            sum += val;
        }
        double avg = history.isEmpty() ? 0 : sum / history.size();
        double predictedCpu = avg * 1.05;

        return Forecast.builder()
                .predictedCpu(Double.parseDouble(String.format("%.2f", predictedCpu)))
                .predictedCost(135.50)
                .confidence(0.78)
                .timeframe("+1 Hour")
                .build();
    }
}
