package com.optibrain.prediction.predictor;

import com.optibrain.prediction.model.Forecast;
import java.util.List;

public interface Predictor {
    Forecast predict(List<Double> history);
}
