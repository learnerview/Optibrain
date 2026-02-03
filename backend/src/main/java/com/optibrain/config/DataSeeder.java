package com.optibrain.config;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.model.AuditStatus;
import com.optibrain.audit.repository.AuditLogRepository;
import com.optibrain.autoscaling.model.ActionType;
import com.optibrain.autoscaling.model.SpotAction;
import com.optibrain.autoscaling.model.SpotStatus;
import com.optibrain.autoscaling.repository.SpotActionRepository;
import com.optibrain.cleanup.model.OrphanedResource;
import com.optibrain.cleanup.repository.OrphanedResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DataSeeder for OptiBrain Development and Demo Mode.
 * 
 * WARNING: This component is ONLY active in 'dev' profile.
 * It seeds sample data for testing and demonstration purposes.
 * 
 * PRODUCTION: This seeder will NOT run in production profile.
 * Real production data should come from:
 * - AWS API calls (Cost Explorer, CloudWatch, EC2, etc.)
 * - User-created optimization rules
 * - Actual resource discovery and analysis
 * 
 * To disable in development: Set spring.profiles.active=prod
 */
@Component
@Profile("dev")  // Only run in development profile
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AuditLogRepository auditLogRepository;
    private final SpotActionRepository spotActionRepository;
    private final OrphanedResourceRepository orphanedResourceRepository;
    
    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @Override
    public void run(String... args) {
        if (!demoMode) {
            log.info("ℹ️ [DATA-SEEDER] Demo mode disabled. Skipping sample data generation.");
            return;
        }
        
        log.info("🚀 [DATA-SEEDER] Demo mode enabled. Initializing sample data for development...");
        
        if (auditLogRepository.count() == 0) {
            seedDevelopmentData();
            log.info("✅ [DATA-SEEDER] Development sample data seeded successfully.");
        } else {
            log.info("ℹ️ [DATA-SEEDER] Data already exists. Skipping initialization.");
        }
    }

    /**
     * Seeds sample data for development and demonstration:
     * 1. Sample audit logs (anomaly detection, compliance)
     * 2. Sample optimization recommendations
     * 3. Sample cleanup opportunities
     * 
     * This data mimics what real AWS discovery would produce.
     */
    private void seedDevelopmentData() {
        String tenantId = "default-tenant";

        // --- SAMPLE: ANOMALIES & AUDIT LOGS ---
        AuditLog anomalyEvent = AuditLog.builder()
                .action("EC2_COST_SPIKE_DETECTED")
                .resourceId("i-023ab-sample")
                .status(AuditStatus.PENDING)
                .explanation("Sample: Sudden 116% spike in compute costs. This would be detected from real CloudWatch metrics.")
                .savings(0.0)
                .score(92.0)
                .build();
        anomalyEvent.setTenantId(tenantId);

        AuditLog complianceIssue = AuditLog.builder()
                .action("S3_UNENCRYPTED_BUCKET")
                .resourceId("sample-bucket-2025")
                .status(AuditStatus.SUCCESS)
                .explanation("Sample: Unencrypted S3 bucket detected. Auto-encryption would be recommended.")
                .savings(0.0)
                .score(100.0)
                .build();
        complianceIssue.setTenantId(tenantId);
        
        auditLogRepository.saveAll(List.of(anomalyEvent, complianceIssue));

        // --- SAMPLE: OPTIMIZATION RECOMMENDATIONS ---
        SpotAction rightsizingRec = SpotAction.builder()
                .type(ActionType.MIGRATE_TO_SPOT)
                .resourceId("i-023ab-sample")
                .status(SpotStatus.PENDING)
                .predictedSavings(8400.0)
                .build();
        rightsizingRec.setTenantId(tenantId);
        
        SpotAction storageRec = SpotAction.builder()
                .type(ActionType.DIVERSITY_BALANCE)
                .resourceId("sample-db-rds")
                .status(SpotStatus.COMPLETED)
                .predictedSavings(4600.0)
                .build();
        storageRec.setTenantId(tenantId);
        
        spotActionRepository.saveAll(List.of(rightsizingRec, storageRec));

        // --- SAMPLE: CLEANUP OPPORTUNITIES ---
        OrphanedResource idleVolume = OrphanedResource.builder()
                .resourceId("vol-sample123")
                .resourceType("EBS_VOLUME")
                .region("us-east-1")
                .estimatedMonthlyCost(120.0)
                .resolved(false)
                .build();
        idleVolume.setTenantId(tenantId);
        
        OrphanedResource unattachedEIP = OrphanedResource.builder()
                .resourceId("eipalloc-sample")
                .resourceType("ELASTIC_IP")
                .region("us-west-2")
                .estimatedMonthlyCost(25.0)
                .resolved(true)
                .build();
        unattachedEIP.setTenantId(tenantId);
        
        orphanedResourceRepository.saveAll(List.of(idleVolume, unattachedEIP));
        
        log.info("📊 [DATA-SEEDER] Seeded: {} audit logs, {} recommendations, {} cleanup items",
                2, 2, 2);
    }
}
