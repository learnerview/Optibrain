package com.optibrain.metrics.provider;

import com.optibrain.metrics.model.MetricData;
import com.optibrain.metrics.model.MetricSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Mock Metrics Provider for Testing and Development
 * 
 * PURPOSE: This provider generates random fake metrics for:
 * - Unit testing
 * - Integration testing without real cloud infrastructure
 * - UI development without CloudWatch costs
 * 
 * USAGE:
 * - Set cloud.mode=MOCK in application properties
 * - Used automatically when real metrics are unavailable
 * 
 * PRODUCTION WARNING:
 * This provider should NEVER be used in production.
 * It returns random generated data that does not reflect real infrastructure.
 * 
 * For production, use:
 * - AwsMetricsProvider (for real AWS CloudWatch)
 * - LocalstackMetricsProvider (for local testing with LocalStack)
 * - PrometheusMetricsProvider (for Prometheus/Grafana setups)
 */
@Component
@Profile({"dev", "test"})  // Only available in dev/test profiles
@Slf4j
public class MockMetricsProvider implements MetricsProvider {

    @Override
    public List<MetricData> getCurrentMetrics() {
        log.debug("[MOCK] Generating random fake metrics for development");
        
        // Generate random metrics for demo purposes
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
                    com.optibrain.metrics.model.MetricSnapshot.builder()
                        .timestamp(Instant.now().minusSeconds(3600))
                        .cpu(65.0)
                        .memory(60.0)
                        .build(),
                    com.optibrain.metrics.model.MetricSnapshot.builder()
                        .timestamp(Instant.now().minusSeconds(7200))
                        .cpu(68.0)
                        .memory(62.0)
                        .build(),
                    com.optibrain.metrics.model.MetricSnapshot.builder()
                        .timestamp(Instant.now().minusSeconds(10800))
                        .cpu(72.0)
                        .memory(65.0)
                        .build()
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
        log.debug("[MOCK] Generating fake metric snapshot for tenant: {}", tenantId);
        
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
