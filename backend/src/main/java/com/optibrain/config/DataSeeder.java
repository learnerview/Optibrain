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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DataSeeder for OptiBrain Demo Mode.
 * Seeds the 4 vertical demo pillars with consistent, deterministic data.
 * Pruned of unstable ML/FinancialReport entities for hackathon stability.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AuditLogRepository auditLogRepository;
    private final SpotActionRepository spotActionRepository;
    private final OrphanedResourceRepository orphanedResourceRepository;

    @Override
    public void run(String... args) {
        log.info("🚀 [DEMO-SEEDER] Initializing OptiBrain Story Data...");
        
        if (auditLogRepository.count() == 0) {
            seedStoryData();
            log.info("✅ [DEMO-SEEDER] Story Data Seeded Successfully.");
        } else {
            log.info("ℹ️ [DEMO-SEEDER] Data already exists. Skipping initialization.");
        }
    }

    /**
     * Seeds the demo story:
     * 1. A High-Severity Anomaly (AuditLog)
     * 2. A Corresponding Recommendation (SpotAction)
     * 3. Cleanup Actions (OrphanedResource)
     */
    private void seedStoryData() {
        String tenantId = "demo-tenant";

        // --- STORY PILLAR: ANOMALIES & SECURITY ---
        // Aligns with the "Sudden increase in on-demand EC2 usage" story
        AuditLog anomalyEvent = AuditLog.builder()
                .action("EC2_COST_SPIKE_DETECTED")
                .resourceId("i-023ab-prod")
                .status(AuditStatus.PENDING)
                .explanation("Sudden 116% spike in on-demand compute costs detected in us-east-1. Potential mis-scaled Autoscaling Group.")
                .savings(0.0)
                .score(92.0)
                .build();
        anomalyEvent.setTenantId(tenantId);

        AuditLog complianceIssue = AuditLog.builder()
                .action("S3_UNENCRYPTED_BUCKET")
                .resourceId("optibrain-archive-2025")
                .status(AuditStatus.SUCCESS)
                .explanation("Sensitivity high: Auto-encryption enabled via policy.")
                .savings(0.0)
                .score(100.0)
                .build();
        complianceIssue.setTenantId(tenantId);
        
        auditLogRepository.saveAll(List.of(anomalyEvent, complianceIssue));

        // --- STORY PILLAR: RECOMMENDATIONS ---
        // Aligns with the "Downscale or stop instance" recommendation
        SpotAction rightsizingRec = SpotAction.builder()
                .type(ActionType.MIGRATE_TO_SPOT)
                .resourceId("i-023ab-prod")
                .status(SpotStatus.PENDING)
                .predictedSavings(8400.0) // Matches recommendations.json
                .build();
        rightsizingRec.setTenantId(tenantId);
        
        SpotAction storageRec = SpotAction.builder()
                .type(ActionType.DIVERSITY_BALANCE)
                .resourceId("db-prod-rds")
                .status(SpotStatus.COMPLETED)
                .predictedSavings(4600.0)
                .build();
        storageRec.setTenantId(tenantId);
        
        spotActionRepository.saveAll(List.of(rightsizingRec, storageRec));

        // --- STORY PILLAR: SAVINGS & CLEANUP ---
        // Aligns with the "Resource cleanup" story
        OrphanedResource idleVolume = OrphanedResource.builder()
                .resourceId("vol-0af123")
                .resourceType("EBS_VOLUME")
                .region("us-east-1")
                .estimatedMonthlyCost(120.0)
                .resolved(false)
                .build();
        idleVolume.setTenantId(tenantId);
        
        OrphanedResource unattachedEIP = OrphanedResource.builder()
                .resourceId("eipalloc-01234")
                .resourceType("ELASTIC_IP")
                .region("us-west-2")
                .estimatedMonthlyCost(25.0)
                .resolved(true)
                .build();
        unattachedEIP.setTenantId(tenantId);
        
        orphanedResourceRepository.saveAll(List.of(idleVolume, unattachedEIP));
    }
}
