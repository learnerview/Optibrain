package com.optibrain.metrics.provider;

import com.optibrain.metrics.model.MetricData;
import com.optibrain.metrics.model.MetricSnapshot;
import java.util.List;

public interface MetricsProvider {
    List<MetricData> getCurrentMetrics();
    String getProviderSource();
    MetricSnapshot getMetricSnapshot(String tenantId);
}
