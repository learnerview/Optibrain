package com.optibrain.cleanup.scheduler;

import com.optibrain.cleanup.model.OrphanedResource;
import com.optibrain.cleanup.service.CleanupService;
import com.optibrain.config.model.TenantConfiguration;
import com.optibrain.config.service.TenantConfigurationService;
import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final TenantCredentialService tenantService;
    private final TenantConfigurationService configService;
    private final CleanupService cleanupService;

    /**
     * Runs daily at midnight to detect and optionally clean up orphaned resources.
     * Cron expression: "0 0 0 * * ?"
     * For demonstration/test purposes, we'll use fixedDelay (every 24 hours).
     */
    @Scheduled(fixedDelay = 86400000)
    public void runDailyCleanup() {
        log.info("Starting scheduled cleanup task for all tenants...");
        
        List<Tenant> tenants = tenantService.getAllTenants();
        for (Tenant tenant : tenants) {
            try {
                processTenantCleanup(tenant.getId());
            } catch (Exception e) {
                log.error("Failed to process cleanup for tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
        
        log.info("Scheduled cleanup task completed.");
    }

    private void processTenantCleanup(String tenantId) {
        com.optibrain.common.context.TenantContext.setTenantId(tenantId);
        try {
            TenantConfiguration config = configService.getConfiguration(tenantId);
            
            log.info("Analyzing resources for tenant: {}", tenantId);
            // CleanupService.detectOrphanedResources now takes no arguments and uses context
            cleanupService.detectOrphanedResources();
            
            // Re-fetch or use results from detectOrphanedResources (which returns them)
            List<com.optibrain.cleanup.dto.OrphanedResourceResponseDTO> resources = cleanupService.detectOrphanedResources();
            
            if (resources.isEmpty()) {
                log.info("No orphaned resources found for tenant: {}", tenantId);
                return;
            }

            log.info("Found {} orphaned resources for tenant: {}", resources.size(), tenantId);

            if (config.isAutoCleanupEnabled()) {
                log.info("Auto-cleanup is ENABLED for tenant: {}. Proceeding with deletion...", tenantId);
                for (var resource : resources) {
                    if (!resource.resolved()) {
                        String result = cleanupService.executeCleanup(
                                resource.resourceId(), 
                                resource.resourceType(), 
                                false // Execute for real
                        );
                        log.info("Cleanup Result for {}: {}", resource.resourceId(), result);
                    }
                }
            } else {
                log.info("Auto-cleanup is DISABLED for tenant: {}. Resources logged only.", tenantId);
            }
        } finally {
            com.optibrain.common.context.TenantContext.clear();
        }
    }
}
