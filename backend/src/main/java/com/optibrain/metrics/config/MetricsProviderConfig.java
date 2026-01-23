package com.optibrain.metrics.config;

import com.optibrain.cloud.config.CloudConfig;
import com.optibrain.metrics.provider.LocalstackMetricsProvider;
import com.optibrain.metrics.provider.MockMetricsProvider;
import com.optibrain.metrics.provider.PrometheusMetricsProvider;
import com.optibrain.metrics.provider.AwsMetricsProvider;
import com.optibrain.metrics.provider.MetricsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MetricsProviderConfig {

    private final CloudConfig cloudConfig;

    @Bean
    public MetricsProvider metricsProvider(
            LocalstackMetricsProvider localstackMetricsProvider,
            MockMetricsProvider mockMetricsProvider,
            PrometheusMetricsProvider prometheusMetricsProvider,
            AwsMetricsProvider awsMetricsProvider) {
        switch (cloudConfig.getMode().toUpperCase()) {
            case "LOCALSTACK":
                return localstackMetricsProvider;
            case "PROMETHEUS":
                return prometheusMetricsProvider;
            case "AWS":
                return awsMetricsProvider;
            case "MOCK":
            default:
                return mockMetricsProvider;
        }
    }
}
