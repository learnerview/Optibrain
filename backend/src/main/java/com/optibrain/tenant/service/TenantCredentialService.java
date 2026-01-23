package com.optibrain.tenant.service;

import com.optibrain.tenant.model.CloudCredentials;
import com.optibrain.tenant.model.Tenant;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class TenantCredentialService {

    public List<Tenant> getAllTenants() {
        return Collections.singletonList(
            Tenant.builder()
                .id("tenant-1")
                .name("Demo Tenant")
                .active(true)
                .cloudProvider("AWS")
                .mode("AUTONOMOUS")
                .plan("ENTERPRISE")
                .settings(Tenant.TenantSettings.builder()
                    .autonomousMode(true)
                    .monthlyBudgetLimit(1000.0)
                    .protectedResources(Collections.emptyList())
                    .build())
                .build()
        );
    }
    
    public Tenant getTenant(String tenantId) {
        return Tenant.builder()
            .id(tenantId)
            .name("Demo Tenant")
            .active(true)
            .cloudProvider("AWS")
            .mode("AUTONOMOUS")
            .plan("ENTERPRISE")
            .settings(Tenant.TenantSettings.builder()
                .autonomousMode(true)
                .monthlyBudgetLimit(1000.0)
                .protectedResources(Collections.emptyList())
                .build())
            .build();
    }

    public CloudCredentials getCredentials(String tenantId) {
        return CloudCredentials.builder()
            .accessKeyId("AKIAVFAKEKEY")
            .secretAccessKey("fakeSecretKey")
            .region("us-east-1")
            .build();
    }

    public void updateTenantActivity(String tenantId) {
        // No-op for hackathon
    }
}
