package com.optibrain.cloud.adapter;

import com.optibrain.cloud.model.CloudAction;
import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantAwareCloudAdapterFactory {
    
    private final TenantCredentialService tenantCredentialService;
    
    public CloudAdapter getAdapter(String tenantId) {
        try {
            Tenant tenant = tenantCredentialService.getTenant(tenantId);
            if (tenant == null) {
                log.warn("Tenant not found: {}, using mock adapter", tenantId);
                return new MockCloudAdapter();
            }
            
            return createAdapterForTenant(tenant);
            
        } catch (Exception e) {
            log.error("Error creating adapter for tenant {}: {}", tenantId, e.getMessage());
            return new MockCloudAdapter();
        }
    }
    
    private CloudAdapter createAdapterForTenant(Tenant tenant) {
        String cloudProvider = tenant.getCloudProvider();
        String mode = tenant.getMode();
        
        if ("mock".equalsIgnoreCase(mode)) {
            return new MockCloudAdapter();
        } else if ("aws".equalsIgnoreCase(cloudProvider)) {
            return new TenantAwareAwsAdapter(tenant);
        } else {
            log.warn("Unsupported cloud provider: {}, using mock adapter", cloudProvider);
            return new MockCloudAdapter();
        }
    }
    
    public static class TenantAwareAwsAdapter implements CloudAdapter {
        private final Tenant tenant;
        private final software.amazon.awssdk.services.ec2.Ec2Client ec2Client;
        
        public TenantAwareAwsAdapter(Tenant tenant) {
            this.tenant = tenant;
            this.ec2Client = createEc2Client();
        }
        
        private software.amazon.awssdk.services.ec2.Ec2Client createEc2Client() {
            // Use default credentials provider (will use env vars, IAM role, or AWS credentials file)
            // In production, each tenant should have their own IAM role or credentials
            software.amazon.awssdk.regions.Region region = software.amazon.awssdk.regions.Region.US_EAST_1;
            
            return software.amazon.awssdk.services.ec2.Ec2Client.builder()
                .region(region)
                .credentialsProvider(software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create())
                .build();
        }
        
        @Override
        public String getProviderName() {
            return "AWS_TENANT_" + tenant.getId();
        }
        
        @Override
        public java.util.List<String> discoverInstances() {
            try {
                software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse response = 
                    ec2Client.describeInstances();
                return response.reservations().stream()
                    .flatMap(r -> r.instances().stream())
                    .map(software.amazon.awssdk.services.ec2.model.Instance::instanceId)
                    .collect(java.util.stream.Collectors.toList());
            } catch (Exception e) {
                log.error("[TENANT:{}] Failed to discover instances: {}", tenant.getId(), e.getMessage());
                return java.util.List.of();
            }
        }
        
        @Override
        public boolean scaleUp(String resourceId) {
            log.info("[TENANT:{}] Scaling UP {} - Auto-scaling via ASG not implemented", tenant.getId(), resourceId);
            // TODO: Implement auto-scaling group desired capacity update
            return true;
        }
        
        @Override
        public boolean scaleDown(String resourceId) {
            log.info("[TENANT:{}] Scaling DOWN {} - Auto-scaling via ASG not implemented", tenant.getId(), resourceId);
            // TODO: Implement auto-scaling group desired capacity update
            return true;
        }
        
        @Override
        public boolean terminateResource(String resourceId) {
            try {
                log.info("[TENANT:{}] Terminating resource {}", tenant.getId(), resourceId);
                software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest request = 
                    software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest.builder()
                        .instanceIds(resourceId)
                        .build();
                ec2Client.terminateInstances(request);
                return true;
            } catch (Exception e) {
                log.error("[TENANT:{}] Failed to terminate {}: {}", tenant.getId(), resourceId, e.getMessage());
                return false;
            }
        }
        
        @Override
        public double getCostEstimate(String resourceId) {
            // Basic cost estimation based on instance type
            // In production, this should integrate with AWS Pricing API
            try {
                var specs = getCurrentSpecs(resourceId);
                String instanceType = specs.get("instanceType");
                if (instanceType != null) {
                    // Simplified cost map - should be from AWS Pricing API
                    return estimateCostByType(instanceType);
                }
            } catch (Exception e) {
                log.warn("[TENANT:{}] Could not estimate cost for {}: {}", tenant.getId(), resourceId, e.getMessage());
            }
            return 0.0;
        }
        
        private double estimateCostByType(String instanceType) {
            // Approximate hourly costs for common instance types (us-east-1)
            // TODO: Replace with AWS Pricing API integration
            if (instanceType.startsWith("t3.")) {
                if (instanceType.equals("t3.nano")) return 0.0052;
                if (instanceType.equals("t3.micro")) return 0.0104;
                if (instanceType.equals("t3.small")) return 0.0208;
                if (instanceType.equals("t3.medium")) return 0.0416;
                if (instanceType.equals("t3.large")) return 0.0832;
                if (instanceType.equals("t3.xlarge")) return 0.1664;
            }
            return 0.05; // Default estimate
        }
        
        @Override
        public String getResourceType(String resourceId) {
            try {
                var specs = getCurrentSpecs(resourceId);
                return specs.getOrDefault("instanceType", "unknown");
            } catch (Exception e) {
                log.warn("[TENANT:{}] Could not get resource type for {}: {}", tenant.getId(), resourceId, e.getMessage());
                return "unknown";
            }
        }
        
        public void executeAction(CloudAction action) {
            log.info("Executing AWS action for tenant {}: {}", tenant.getId(), action.getType());
            // Implementation for AWS-specific actions
        }
        
        @Override
        public java.util.Map<String, String> getCurrentSpecs(String resourceId) {
            try {
                software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest request = 
                    software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest.builder()
                        .instanceIds(resourceId)
                        .build();
                software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse response = 
                    ec2Client.describeInstances(request);
                
                if (response.reservations().isEmpty() || response.reservations().get(0).instances().isEmpty()) {
                    return java.util.Map.of();
                }
                
                software.amazon.awssdk.services.ec2.model.Instance instance = 
                    response.reservations().get(0).instances().get(0);
                
                return java.util.Map.of(
                    "provider", "aws",
                    "tenantId", tenant.getId(),
                    "resourceId", resourceId,
                    "instanceType", instance.instanceType().toString(),
                    "state", instance.state().nameAsString(),
                    "availabilityZone", instance.placement().availabilityZone()
                );
            } catch (Exception e) {
                log.error("[TENANT:{}] Failed to get specs for {}: {}", tenant.getId(), resourceId, e.getMessage());
                return java.util.Map.of(
                    "provider", "aws",
                    "tenantId", tenant.getId(),
                    "resourceId", resourceId,
                    "error", e.getMessage()
                );
            }
        }
        
        public String getProvider() {
            return "aws";
        }
    }
    
    public static class MockCloudAdapter implements CloudAdapter {
        @Override
        public String getProviderName() {
            return "MOCK_ADAPTER";
        }
        
        @Override
        public java.util.List<String> discoverInstances() {
            return java.util.List.of("mock-instance-1", "mock-instance-2");
        }
        
        @Override
        public boolean scaleUp(String resourceId) {
            log.info("Mock scaling UP {}", resourceId);
            return true;
        }
        
        @Override
        public boolean scaleDown(String resourceId) {
            log.info("Mock scaling DOWN {}", resourceId);
            return true;
        }
        
        @Override
        public boolean terminateResource(String resourceId) {
            log.info("Mock terminating {}", resourceId);
            return true;
        }
        
        @Override
        public double getCostEstimate(String resourceId) {
            return 10.0;
        }
        
        @Override
        public String getResourceType(String resourceId) {
            return "mock-instance";
        }
        
        public void executeAction(CloudAction action) {
            log.info("Mock execution of action: {}", action.getType());
        }
        
        @Override
        public java.util.Map<String, String> getCurrentSpecs(String resourceId) {
            return java.util.Map.of(
                "provider", "mock",
                "resourceId", resourceId,
                "status", "mock"
            );
        }
        
        public String getProvider() {
            return "mock";
        }
    }
}
