package com.optibrain.metrics.provider;

import com.optibrain.metrics.model.MetricData;
import com.optibrain.metrics.model.MetricSnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Component
public class MockMetricsProvider implements MetricsProvider {

    @Override
    public List<MetricData> getCurrentMetrics() {
        double cpu = 70.0 + (Math.random() * 10);
        double memory = 60.0 + (Math.random() * 10);
        
        MetricData data = MetricData.builder()
                .cpuUtilization(cpu)
                .memoryUtilization(memory)
                .hourlyCost(120.50 + (Math.random() * 5))
                .instanceCount(4)
                .timestamp(Instant.now())
                .region("us-east-1")
                .serviceBreakdown(java.util.Map.of(
                    "EC2", 80.5,
                    "RDS", 30.0,
                    "S3", 10.0
                ))
                .historicalData(java.util.List.of(
                    com.optibrain.metrics.model.MetricSnapshot.builder().timestamp(Instant.now().minusSeconds(3600)).cpu(65.0).memory(60.0).build(),
                    com.optibrain.metrics.model.MetricSnapshot.builder().timestamp(Instant.now().minusSeconds(7200)).cpu(68.0).memory(62.0).build(),
                    com.optibrain.metrics.model.MetricSnapshot.builder().timestamp(Instant.now().minusSeconds(10800)).cpu(72.0).memory(65.0).build()
                ))
                .build();
        
        return Collections.singletonList(data);
    }

    @Override
    public String getProviderSource() {
        return "MOCK_INTERNAL";
    }
    
    @Override
    public MetricSnapshot getMetricSnapshot(String tenantId) {
        List<MetricData> metrics = getCurrentMetrics();
        return MetricSnapshot.builder()
                .tenantId(tenantId)
                .timestamp(Instant.now())
                .metrics(metrics)
                .totalCost(metrics.stream()
                        .filter(m -> m.getName() != null && m.getName().contains("Cost"))
                        .mapToDouble(MetricData::getValue)
                        .sum())
                .build();
    }
}
