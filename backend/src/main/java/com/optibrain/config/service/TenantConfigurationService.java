package com.optibrain.config.service;

import com.optibrain.config.model.TenantConfiguration;
import com.optibrain.config.repository.TenantConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantConfigurationService {

    private final TenantConfigurationRepository configurations;

    /**
     * The tenant's stored configuration, or the platform defaults for a tenant that has
     * never been tuned. Defaults are returned, not persisted: a read must not create a
     * row.
     */
    @Transactional(readOnly = true)
    public TenantConfiguration getConfiguration(String tenantId) {
        return configurations.findById(tenantId).orElseGet(() -> defaults(tenantId));
    }

    /**
     * Stores the tenant's configuration.
     *
     * <p>A supplied configuration naming a different tenant is refused rather than
     * silently reparented: the path tenant owns the row, and accepting a mismatched body
     * would let one tenant overwrite another's settings.
     */
    @Transactional
    public TenantConfiguration updateConfiguration(String tenantId, TenantConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration is required");
        }
        if (config.getTenantId() != null && !config.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException(
                    "Configuration for tenant '" + config.getTenantId()
                            + "' cannot be updated as tenant '" + tenantId + "'");
        }
        config.setTenantId(tenantId);
        return configurations.save(config);
    }

    private TenantConfiguration defaults(String tenantId) {
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
}
