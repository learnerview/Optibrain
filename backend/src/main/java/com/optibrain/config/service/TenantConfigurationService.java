package com.optibrain.config.service;

import com.optibrain.config.model.TenantConfiguration;
import org.springframework.stereotype.Service;

@Service
public class TenantConfigurationService {

    public TenantConfiguration getConfiguration(String tenantId) {
        return TenantConfiguration.builder()
                .tenantId(tenantId)
                .cpuUpperBound(80.0)
                .cpuLowerBound(20.0)
                .scalingStrategy("BALANCED")
                .enableML(true)
                .riskThreshold(0.6)
                .riPreferredTerm(1)
                .riPaymentOption("NO_UPFRONT")
                .ebsUnusedDaysThreshold(30)
                .snapshotOldDaysThreshold(7)
                .autoCleanupEnabled(false)
                .build();
    }
    
    public TenantConfiguration updateConfiguration(String tenantId, TenantConfiguration config) {
        // In real implementation, this would update the database
        // For now, just return the updated config
        config.setTenantId(tenantId);
        return config;
    }
}
