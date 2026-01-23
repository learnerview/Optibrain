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
        
        public TenantAwareAwsAdapter(Tenant tenant) {
            this.tenant = tenant;
        }
        
        @Override
        public String getProviderName() {
            return "AWS_TENANT_" + tenant.getId();
        }
        
        @Override
        public java.util.List<String> discoverInstances() {
            return java.util.List.of("i-1234567890abcdef0", "i-0987654321fedcba9");
        }
        
        @Override
        public boolean scaleUp(String resourceId) {
            log.info("[TENANT:{}] Scaling UP {}", tenant.getId(), resourceId);
            return true;
        }
        
        @Override
        public boolean scaleDown(String resourceId) {
            log.info("[TENANT:{}] Scaling DOWN {}", tenant.getId(), resourceId);
            return true;
        }
        
        @Override
        public boolean terminateResource(String resourceId) {
            log.info("[TENANT:{}] Terminating {}", tenant.getId(), resourceId);
            return true;
        }
        
        @Override
        public double getCostEstimate(String resourceId) {
            return 25.50; // Placeholder
        }
        
        @Override
        public String getResourceType(String resourceId) {
            return "t3.medium";
        }
        
        public void executeAction(CloudAction action) {
            // Implementation for AWS-specific actions
            log.info("Executing AWS action for tenant {}: {}", tenant.getId(), action.getType());
        }
        
        @Override
        public java.util.Map<String, String> getCurrentSpecs(String resourceId) {
            return java.util.Map.of(
                "provider", "aws",
                "tenantId", tenant.getId(),
                "resourceId", resourceId,
                "status", "active"
            );
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
