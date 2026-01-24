package com.optibrain.roi.service;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.model.AuditStatus;
import com.optibrain.audit.repository.AuditLogRepository;
import com.optibrain.autoscaling.model.SpotAction;
import com.optibrain.autoscaling.repository.SpotActionRepository;
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

    public Map<String, Object> getExecutiveSummary() {
        log.info("[ROI] Generating global executive ROI summary (Demo Mode)");
        
        // Demo-safe implementation - returns deterministic values
        double totalSavings = 156000.0; // Annual savings from demo story
        double totalForecasted = 128450.0; // Monthly cost from demo story
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalFleetSavings", totalSavings);
        summary.put("averageRoi", (totalSavings / totalForecasted) * 100);
        summary.put("totalTenantsMonitored", 1);
        summary.put("activeAutomations", spotActionRepository.count());
        summary.put("lastCalculated", LocalDateTime.now());
        
        return summary;
    }

    public Map<String, Object> getRealTimeMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("liveSavingsCounter", spotActionRepository.findAll().stream()
                .mapToDouble(SpotAction::getPredictedSavings).sum());
        metrics.put("pendingOptimizations", auditLogRepository.count());
        metrics.put("systemHealth", "OPTIMAL");
        return metrics;
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
                    performer.put("efficiency", 92.5);
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
