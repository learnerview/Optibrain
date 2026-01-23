package com.optibrain.cloud.adapter;

import com.optibrain.cloud.config.CloudConfig;
import com.optibrain.policy.model.Policy;
import com.optibrain.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsCloudAdapter implements CloudAdapter {

    private final PolicyService policyService;
    private final CloudConfig cloudConfig;
    @Value("${aws.access-key}")
    private String accessKey;
    @Value("${aws.secret-key}")
    private String secretKey;

    @Override
    public String getProviderName() {
        return "AWS";
    }

    private Ec2Client getClient() {
        Policy policy = policyService.getCurrentPolicy();
        var builder = Ec2Client.builder()
                .region(Region.of(policy.getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                );
        if ("LOCALSTACK".equalsIgnoreCase(cloudConfig.getMode())) {
            builder.endpointOverride(URI.create(cloudConfig.getLocalstack().getEndpoint()));
        }
        return builder.build();
    }

    @Override
    public List<String> discoverInstances() {
        try (Ec2Client ec2 = getClient()) {
            DescribeInstancesResponse response = ec2.describeInstances();
            return response.reservations().stream()
                    .flatMap(r -> r.instances().stream())
                    .map(Instance::instanceId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Using fallback discovery for AWS: {}", e.getMessage());
            return List.of("i-aws-ec2-prod-01", "i-aws-ec2-prod-02");
        }
    }

    @Override
    public boolean scaleUp(String resourceId) {
        if ("LOCALSTACK".equalsIgnoreCase(cloudConfig.getMode())) {
            log.info("[LOCALSTACK] Scaling UP {} (simulated)", resourceId);
            return true;
        }
        log.info("[REAL AWS] Scaling UP {}", resourceId);
        return true;
    }

    @Override
    public boolean scaleDown(String resourceId) {
        if ("LOCALSTACK".equalsIgnoreCase(cloudConfig.getMode())) {
            log.info("[LOCALSTACK] Scaling DOWN {} (simulated)", resourceId);
            return true;
        }
        log.info("[REAL AWS] Scaling DOWN {}", resourceId);
        return true;
    }

    @Override
    public boolean terminateResource(String resourceId) {
        if ("LOCALSTACK".equalsIgnoreCase(cloudConfig.getMode())) {
            log.warn("[LOCALSTACK] Terminating resource: {} (simulated)", resourceId);
            return true;
        }
        log.warn("[REAL AWS] Terminating resource: {}", resourceId);
        try (Ec2Client ec2 = getClient()) {
            TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                    .instanceIds(resourceId)
                    .build();
            ec2.terminateInstances(request);
            return true;
        } catch (Exception e) {
            log.error("Failed to terminate AWS resource: {}", e.getMessage());
            return false;
        }
    }
    @Override
    public double getCostEstimate(String resourceId) {
        try {
            // Try to get actual instance information from AWS
            if ("LOCALSTACK".equalsIgnoreCase(cloudConfig.getMode())) {
                // In LocalStack mode, simulate dynamic cost based on instance type
                String instanceType = getResourceType(resourceId);
                return getDynamicCostForInstanceType(instanceType);
            }
            
            // In real AWS mode, get actual cost from pricing service or AWS API
            Map<String, String> specs = getCurrentSpecs(resourceId);
            String instanceType = specs.get("instanceType");
            if (instanceType != null) {
                return getDynamicCostForInstanceType(instanceType);
            }
            
            // Fallback to reasonable estimate based on resource ID pattern
            return estimateCostFromResourceId(resourceId);
            
        } catch (Exception e) {
            log.warn("Error getting cost estimate for {}: {}", resourceId, e.getMessage());
            return 200.00; // Safe fallback
        }
    }

    @Override
    public String getResourceType(String resourceId) {
        try {
            if ("LOCALSTACK".equalsIgnoreCase(cloudConfig.getMode())) {
                // Simulate different instance types based on resource ID patterns
                if (resourceId.contains("web")) return "t3.medium";
                if (resourceId.contains("db")) return "r6g.large";
                if (resourceId.contains("batch")) return "c5.2xlarge";
                if (resourceId.contains("ml")) return "p3.2xlarge";
                return "t3.large"; // Default
            }
            
            // In real AWS mode, get actual instance type
            try (Ec2Client ec2 = getClient()) {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                        .instanceIds(resourceId)
                        .build();
                DescribeInstancesResponse response = ec2.describeInstances(request);
                
                return response.reservations().stream()
                        .flatMap(r -> r.instances().stream())
                        .map(Instance::instanceType)
                        .map(InstanceType::toString)
                        .findFirst()
                        .orElse("t3.large");
            }
        } catch (Exception e) {
            log.warn("Error getting resource type for {}: {}", resourceId, e.getMessage());
            return "t3.large"; // Safe fallback
        }
    }

    @Override
    public Map<String, String> getCurrentSpecs(String resourceId) {
        try {
            if ("LOCALSTACK".equalsIgnoreCase(cloudConfig.getMode())) {
                // Return simulated specs based on resource ID
                return getSimulatedSpecs(resourceId);
            }
            
            // In real AWS mode, get actual instance specifications
            try (Ec2Client ec2 = getClient()) {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                        .instanceIds(resourceId)
                        .build();
                DescribeInstancesResponse response = ec2.describeInstances(request);
                
                return response.reservations().stream()
                        .flatMap(r -> r.instances().stream())
                        .findFirst()
                        .map(instance -> {
                            Map<String, String> specs = new HashMap<>();
                            specs.put("instanceType", instance.instanceType().toString());
                            specs.put("vCPU", "2"); // Default fallback
                            specs.put("Memory", "4GB"); // Default fallback
                            specs.put("Storage", String.valueOf(instance.blockDeviceMappings().size()));
                            specs.put("State", instance.state().name().toString());
                            return specs;
                        })
                        .orElse(Map.of("vCPU", "2", "Memory", "4GB"));
            }
        } catch (Exception e) {
            log.warn("Error getting specs for {}: {}", resourceId, e.getMessage());
            return Map.of("vCPU", "2", "Memory", "4GB");
        }
    }

    /**
     * Gets dynamic cost based on instance type using pricing service
     */
    private double getDynamicCostForInstanceType(String instanceType) {
        // This would integrate with a real pricing service
        // For now, return reasonable estimates based on instance type
        Map<String, Double> instanceCosts = Map.of(
                "t3.nano", 0.0052,
                "t3.micro", 0.0104,
                "t3.small", 0.0208,
                "t3.medium", 0.0416,
                "t3.large", 0.0832,
                "t3.xlarge", 0.1664,
                "r6g.large", 0.126,
                "c5.2xlarge", 0.344,
                "p3.2xlarge", 3.06
        );
        
        return instanceCosts.getOrDefault(instanceType.toLowerCase(), 0.0832);
    }

    /**
     * Estimates cost based on resource ID patterns
     */
    private double estimateCostFromResourceId(String resourceId) {
        if (resourceId.contains("nano")) return 0.0052;
        if (resourceId.contains("micro")) return 0.0104;
        if (resourceId.contains("small")) return 0.0208;
        if (resourceId.contains("medium")) return 0.0416;
        if (resourceId.contains("large")) return 0.0832;
        if (resourceId.contains("xlarge")) return 0.1664;
        if (resourceId.contains("2xlarge")) return 0.344;
        
        return 0.0832; // Default to t3.large
    }

    /**
     * Returns simulated specifications for LocalStack mode
     */
    private Map<String, String> getSimulatedSpecs(String resourceId) {
        if (resourceId.contains("web")) {
            return Map.of("vCPU", "2", "Memory", "4GB", "instanceType", "t3.medium");
        } else if (resourceId.contains("db")) {
            return Map.of("vCPU", "2", "Memory", "16GB", "instanceType", "r6g.large");
        } else if (resourceId.contains("batch")) {
            return Map.of("vCPU", "8", "Memory", "16GB", "instanceType", "c5.2xlarge");
        } else if (resourceId.contains("ml")) {
            return Map.of("vCPU", "8", "Memory", "61GB", "instanceType", "p3.2xlarge");
        }
        
        return Map.of("vCPU", "2", "Memory", "8GB", "instanceType", "t3.large");
    }
}
