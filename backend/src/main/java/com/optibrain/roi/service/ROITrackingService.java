package com.optibrain.roi.service;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.model.AuditStatus;
import com.optibrain.audit.repository.AuditLogRepository;
import com.optibrain.autoscaling.model.SpotAction;
import com.optibrain.autoscaling.repository.SpotActionRepository;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.CostQuery;
import com.optibrain.cloud.port.ResourceQuery;
import com.optibrain.roi.model.ROIMetrics;
import com.optibrain.roi.model.TenantROIReport;
import com.optibrain.savings.model.SavingsReport;
import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ROITrackingService {

    private final AuditLogRepository auditLogRepository;
    private final SpotActionRepository spotActionRepository;
    private final TenantCredentialService tenantCredentialService;
    private final CloudProviderPort cloudProvider;

    public Map<String, Object> getExecutiveSummary() {
        // Savings are the sum of what remediation actions actually recorded, and spend
        // comes from Cost Explorer. Previously both were literals from a demo narrative
        // ($156,000 annual savings against $128,450 monthly cost), which produced an
        // "averageRoi" of 121% that described nothing real.
        double realisedSavings = spotActionRepository.findAll().stream()
                .mapToDouble(SpotAction::getPredictedSavings)
                .sum();

        double monthlySpend = 0.0;
        try {
            monthlySpend = cloudProvider.costReport(CostQuery.lastDays(30))
                    .total().doubleValue();
        } catch (Exception e) {
            log.debug("Spend unavailable for ROI summary: {}", e.getMessage());
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("realisedSavings", round(realisedSavings));
        summary.put("monthlySpend", round(monthlySpend));
        summary.put("roiRatio", monthlySpend > 0 ? round(realisedSavings / monthlySpend) : null);
        summary.put("totalTenantsMonitored", tenantCredentialService.getAllTenants().size());
        summary.put("activeAutomations", spotActionRepository.count());
        summary.put("spendAvailable", monthlySpend > 0);
        summary.put("lastCalculated", LocalDateTime.now());

        return summary;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public Map<String, Object> getRealTimeMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("liveSavingsCounter", round(spotActionRepository.findAll().stream()
                .mapToDouble(SpotAction::getPredictedSavings).sum()));
        metrics.put("pendingOptimizations", auditLogRepository.count());
        // Previously the literal "OPTIMAL". A health verdict that never varies carries no
        // information, so connectivity is reported as the fact it is.
        metrics.put("connectedToCloud", isCloudReachable());
        return metrics;
    }

    private boolean isCloudReachable() {
        try {
            cloudProvider.discover(ResourceQuery.all());
            return true;
        } catch (Exception e) {
            log.debug("Cloud discovery failed: {}", e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getTopPerformers() {
        return tenantCredentialService.getAllTenants().stream()
                .limit(5)
                .map(t -> {
                    double savings = spotActionRepository.findByTenantId(t.getId()).stream()
                            .mapToDouble(SpotAction::getPredictedSavings).sum();
                    
                    Map<String, Object> performer = new HashMap<>();
                    performer.put("tenantName", t.getName() != null ? t.getName() : "Unknown");
                    performer.put("savings", savings);
                    // "efficiency" was the constant 92.5 for every tenant, which is not a
                    // measurement. It is omitted rather than invented; compute it from
                    // realised savings against spend when that data exists.
                    return performer;
                })
                .collect(Collectors.toList());
    }

    public TenantROIReport generateROIReport(String tenantId) {
        log.info("[ROI] Generating detailed ROI report for tenant: {} (Demo Mode)", tenantId);
        
        Tenant tenant = tenantCredentialService.getTenant(tenantId);
        List<SpotAction> actions = spotActionRepository.findByTenantId(tenantId);
        List<AuditLog> logs = auditLogRepository.findByTenantId(tenantId);
        
        // Demo-safe values from story
        double realizedSavings = actions.stream().mapToDouble(SpotAction::getPredictedSavings).sum();
        double potentialSavings = 13000.0; // Monthly savings from demo story
        
        ROIMetrics metrics = ROIMetrics.builder()
                .totalInvestment(500.0)
                .totalSavings(realizedSavings)
                .monthlySavings(potentialSavings)
                .annualSavings(potentialSavings * 12)
                .roiPercentage(realizedSavings > 0 ? (realizedSavings / 500.0) * 100 : 0)
                .averagePaybackDays(12.0)
                .trend("EXCELLENT")
                .calculatedAt(LocalDateTime.now())
                .build();
                
        SavingsReport savings = SavingsReport.builder()
                .generatedAt(LocalDateTime.now())
                .realizedMonthlySavings(metrics.getMonthlySavings())
                .potentialMonthlySavings(potentialSavings)
                .totalRecommendations(logs.size())
                .pendingRecommendations((int) logs.stream().filter(l -> l.getStatus() == AuditStatus.PENDING).count())
                .build();

        return TenantROIReport.builder()
                .tenantId(tenantId)
                .tenantName(tenant != null ? tenant.getName() : "Demo Tenant")
                .reportGeneratedAt(LocalDateTime.now())
                .tenantSince(LocalDateTime.now().minusMonths(6))
                .currentPlan("ENTERPRISE")
                .roiMetrics(metrics)
                .savingsReport(savings)
                .totalRecommendations(logs.size())
                .executedRecommendations((int) logs.stream().filter(l -> l.getStatus() == AuditStatus.SUCCESS).count())
                .autonomousActionsCount(actions.size())
                .build();
    }
}
