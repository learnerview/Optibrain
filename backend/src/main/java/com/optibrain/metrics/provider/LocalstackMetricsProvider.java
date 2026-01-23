package com.optibrain.metrics.provider;

import com.optibrain.metrics.model.MetricData;
import com.optibrain.metrics.model.MetricSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component("localstackMetricsProvider")
@Slf4j
public class LocalstackMetricsProvider implements MetricsProvider {

    private final String endpoint;
    private final String region;
    private final String accessKey;
    private final String secretKey;

    public LocalstackMetricsProvider(
            @org.springframework.beans.factory.annotation.Value("${cloud.localstack.endpoint}") String endpoint,
            @org.springframework.beans.factory.annotation.Value("${cloud.localstack.region}") String region,
            @org.springframework.beans.factory.annotation.Value("${aws.access-key}") String accessKey,
            @org.springframework.beans.factory.annotation.Value("${aws.secret-key}") String secretKey) {
        this.endpoint = endpoint;
        this.region = region;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    private CloudWatchClient getClient() {
        return CloudWatchClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .build();
    }

    @Override
    public List<MetricData> getCurrentMetrics() {
        try (CloudWatchClient cw = getClient()) {
            // Simulated metrics: CPUUtilization, NetworkIn/Out, DiskReadBytes
            double cpu = getMetricStat(cw, "CPUUtilization", "AWS/EC2", 70.0);
            double memory = getMetricStat(cw, "MemoryUtilization", "System/Linux", 60.0);
            double hourlyCost = 120.50 + (Math.random() * 5);
            int instanceCount = getInstanceCount(cw);

            List<MetricSnapshot> history = generateHistory(3);

            MetricData data = MetricData.builder()
                    .cpuUtilization(cpu)
                    .memoryUtilization(memory)
                    .hourlyCost(hourlyCost)
                    .instanceCount(instanceCount)
                    .timestamp(Instant.now())
                    .region(region)
                    .serviceBreakdown(Map.of(
                            "EC2", hourlyCost * 0.70,
                            "RDS", hourlyCost * 0.20,
                            "S3", hourlyCost * 0.10
                    ))
                    .historicalData(history)
                    .build();
            
            return Collections.singletonList(data);
        } catch (Exception e) {
            log.warn("Failed to fetch metrics from LocalStack CloudWatch, falling back to defaults: {}", e.getMessage());
            return Collections.singletonList(fallback());
        }
    }

    private double getMetricStat(CloudWatchClient cw, String metricName, String namespace, double fallback) {
        try {
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace(namespace)
                    .metricName(metricName)
                    .startTime(Instant.now().minusSeconds(3600))
                    .endTime(Instant.now())
                    .period(300)
                    .statistics(Statistic.AVERAGE)
                    .build();

            GetMetricStatisticsResponse response = cw.getMetricStatistics(request);
            Optional<Datapoint> dp = response.datapoints().stream().max(Comparator.comparing(Datapoint::timestamp));
            return dp.map(Datapoint::average).orElse(fallback);
        } catch (Exception e) {
            log.debug("Metric {} not available in LocalStack, using fallback {}", metricName, fallback);
            return fallback + (Math.random() * 10);
        }
    }

    private int getInstanceCount(CloudWatchClient cw) {
        try {
            ListMetricsRequest request = ListMetricsRequest.builder()
                    .namespace("AWS/EC2")
                    .metricName("CPUUtilization")
                    .build();
            ListMetricsResponse response = cw.listMetrics(request);
            return Math.max(1, response.metrics().size());
        } catch (Exception e) {
            return 3;
        }
    }

    private List<MetricSnapshot> generateHistory(int hours) {
        List<MetricSnapshot> list = new ArrayList<>();
        for (int i = hours; i >= 1; i--) {
            list.add(MetricSnapshot.builder()
                    .timestamp(Instant.now().minusSeconds(i * 3600L))
                    .cpu(65 + Math.random() * 10)
                    .memory(55 + Math.random() * 10)
                    .build());
        }
        return list;
    }

    private MetricData fallback() {
        return MetricData.builder()
                .cpuUtilization(70.0 + Math.random() * 10)
                .memoryUtilization(60.0 + Math.random() * 10)
                .hourlyCost(120.50 + Math.random() * 5)
                .instanceCount(3)
                .timestamp(Instant.now())
                .region(region)
                .serviceBreakdown(Map.of("EC2", 80.0, "RDS", 30.0, "S3", 10.0))
                .historicalData(generateHistory(3))
                .build();
    }

    @Override
    public String getProviderSource() {
        return "LOCALSTACK_CLOUDWATCH";
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
