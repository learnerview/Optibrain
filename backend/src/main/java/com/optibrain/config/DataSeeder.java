package com.optibrain.config;

import com.optibrain.analytics.dto.TopSpenderDTO;
import com.optibrain.analytics.model.FinancialReport;
import com.optibrain.analytics.repository.FinancialReportRepository;
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

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final FinancialReportRepository financialReportRepository;
    private final AuditLogRepository auditLogRepository;
    private final SpotActionRepository spotActionRepository;
    private final OrphanedResourceRepository orphanedResourceRepository;

    @Override
    public void run(String... args) {
        log.info("[SEEDER] Checking if demo data exists...");
        
        if (financialReportRepository.count() == 0) {
            seedDemoData();
        } else {
            log.info("[SEEDER] System already seeded. Skipping...");
        }
    }

    private void seedDemoData() {
        log.info("[SEEDER] Seeding comprehensive demo data for 'demo-tenant'...");
        String tenantId = "demo-tenant";

        // 1. Seed Financial Reports
        FinancialReport report = FinancialReport.builder()
                .period("MONTHLY")
                .currentCost(12500.00)
                .previousCost(10800.00)
                .forecastedCost(11500.00)
                .potentialSavings(1850.50)
                .source("ML")
                .topSpenders(List.of(
                    new TopSpenderDTO("az-prod-aks", "Azure_AKS", 4500.0, 36.0),
                    new TopSpenderDTO("aws-ec2-backend", "AWS_EC2", 3200.0, 25.6),
                    new TopSpenderDTO("gcp-bigquery-analytics", "GCP_BigQuery", 2100.0, 16.8)
                ))
                .build();
        report.setTenantId(tenantId);
        financialReportRepository.save(report);

        // 2. Seed Audit Logs (Security/Compliance Issues)
        AuditLog issue1 = AuditLog.builder()
                .action("S3_PUBLIC_ACCESS")
                .resourceId("arn:aws:s3:::optibrain-backups")
                .status(AuditStatus.PENDING)
                .explanation("Public access detected on backup bucket. Critical security risk.")
                .savings(0.0)
                .score(95.0)
                .build();
        issue1.setTenantId(tenantId);
        
        AuditLog issue2 = AuditLog.builder()
                .action("UNRESTRICTED_SSH")
                .resourceId("sg-08ae32194")
                .status(AuditStatus.SUCCESS)
                .explanation("SSH access restricted to internal VPN range.")
                .savings(0.0)
                .score(80.0)
                .build();
        issue2.setTenantId(tenantId);
        
        auditLogRepository.saveAll(List.of(issue1, issue2));

        // 3. Seed Spot Actions (Automation results)
        SpotAction action1 = SpotAction.builder()
                .type(ActionType.MIGRATE_TO_SPOT)
                .resourceId("i-0a2bc4567")
                .status(SpotStatus.COMPLETED)
                .predictedSavings(124.50)
                .build();
        action1.setTenantId(tenantId);
        
        SpotAction action2 = SpotAction.builder()
                .type(ActionType.DIVERSITY_BALANCE)
                .resourceId("az-vm-web-04")
                .status(SpotStatus.PENDING)
                .predictedSavings(45.20)
                .build();
        action2.setTenantId(tenantId);
        
        spotActionRepository.saveAll(List.of(action1, action2));

        // 4. Seed Orphaned Resources (Cleanup recommendations)
        OrphanedResource orphan1 = OrphanedResource.builder()
                .resourceId("vol-09876abc")
                .resourceType("VOLUME")
                .region("us-east-1")
                .estimatedMonthlyCost(35.00)
                .resolved(false)
                .build();
        orphan1.setTenantId(tenantId);
        
        OrphanedResource orphan2 = OrphanedResource.builder()
                .resourceId("snap-44321def")
                .resourceType("SNAPSHOT")
                .region("us-east-1")
                .estimatedMonthlyCost(12.50)
                .resolved(true)
                .build();
        orphan2.setTenantId(tenantId);
        
        orphanedResourceRepository.saveAll(List.of(orphan1, orphan2));

        log.info("[SEEDER] Successfully seeded 7 demo records.");
    }
}
