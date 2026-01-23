package com.optibrain.cloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cloud")
@Data
public class CloudConfig {
    private String mode = "MOCK"; // MOCK | LOCALSTACK | PROMETHEUS | AWS
    private Localstack localstack = new Localstack();
    private boolean dryRun = true; // Safety: default to dry-run

    @Data
    public static class Localstack {
        private String endpoint = "http://localhost:4566";
        private String region = "us-east-1";
    }
}
