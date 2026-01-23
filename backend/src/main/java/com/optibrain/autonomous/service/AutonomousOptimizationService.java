package com.optibrain.autonomous.service;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.service.AuditService;
import com.optibrain.autonomous.model.OptimizationResult;
import com.optibrain.autonomous.repository.OptimizationResultRepository;
import com.optibrain.cloud.config.CloudConfig;
import com.optibrain.decision.scorer.DecisionScorer;
import com.optibrain.metrics.model.MetricData;
import com.optibrain.metrics.provider.TenantAwareMetricsProvider;
import com.optibrain.recommendation.model.Recommendation;
import com.optibrain.recommendation.service.RecommendationService;
import com.optibrain.savings.model.SavingsReport;
import com.optibrain.savings.service.SavingsService;
import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutonomousOptimizationService {
    
    private final TenantCredentialService tenantCredentialService;
    private final TenantAwareMetricsProvider metricsProvider;
    private final RecommendationService recommendationService;
    private final DecisionScorer decisionScorer;
    private final SavingsService savingsService;
    private final AuditService auditService;
    private final CloudConfig cloudConfig;
    private final OptimizationResultRepository optimizationResultRepository;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    // Run optimization cycle every 5 minutes for autonomous mode
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void runAutonomousOptimizationCycle() {
        log.info("Starting autonomous optimization cycle at {}", LocalDateTime.now());
        
        List<CompletableFuture<OptimizationResult>> futures = new ArrayList<>();
        
        // Process all tenants with autonomous mode enabled
        for (Tenant tenant : tenantCredentialService.getAllTenants()) {
            if (tenant.isActive() && tenant.getSettings().isAutonomousMode()) {
                CompletableFuture<OptimizationResult> future = CompletableFuture.supplyAsync(() -> 
                    processTenantOptimization(tenant), executorService);
                futures.add(future);
            }
        }
        
        // Wait for all optimizations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    log.info("Completed autonomous optimization cycle for {} tenants", futures.size());
                    generateOptimizationSummary(futures);
                })
                .exceptionally(throwable -> {
                    log.error("Error in optimization cycle", throwable);
                    return null;
                });
    }
    
    // Run comprehensive analysis every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void runComprehensiveAnalysis() {
        log.info("Starting comprehensive analysis at {}", LocalDateTime.now());
        
        for (Tenant tenant : tenantCredentialService.getAllTenants()) {
            if (tenant.isActive()) {
                try {
                    performComprehensiveAnalysis(tenant);
                } catch (Exception e) {
                    log.error("Comprehensive analysis failed for tenant {}: {}", tenant.getId(), e.getMessage());
                }
            }
        }
    }
    
    // Run orphaned resource cleanup every 6 hours
    @Scheduled(fixedRate = 21600000) // 6 hours
    public void runOrphanedResourceCleanup() {
        log.info("Starting orphaned resource cleanup at {}", LocalDateTime.now());
        
        for (Tenant tenant : tenantCredentialService.getAllTenants()) {
            if (tenant.isActive() && tenant.getSettings().isAutonomousMode()) {
                try {
                    performOrphanedResourceCleanup(tenant);
                } catch (Exception e) {
                    log.error("Orphan cleanup failed for tenant {}: {}", tenant.getId(), e.getMessage());
                }
            }
        }
    }
    
    private OptimizationResult processTenantOptimization(Tenant tenant) {
        log.info("Processing optimization for tenant: {} ({})", tenant.getId(), tenant.getName());
        
        try {
            // Update tenant activity
            tenantCredentialService.updateTenantActivity(tenant.getId());
            
            // Fetch current metrics
            List<MetricData> metricsList = metricsProvider.fetchMetricsForTenant(tenant.getId());
            MetricData metrics = metricsList.isEmpty() ? null : metricsList.get(0);
            
            // Generate recommendations
            List<Recommendation> recommendations = recommendationService.generateRightsizing(metrics);
            
            // Execute approved recommendations automatically in autonomous mode
            int executedCount = 0;
            int totalSavings = 0;
            
            for (Recommendation rec : recommendations) {
                if (shouldExecuteAutomatically(tenant, rec)) {
                    boolean success = executeRecommendation(tenant.getId(), rec);
                    if (success) {
                        executedCount++;
                        totalSavings += rec.getMonthlySavings();
                    }
                }
            }
            
            // Record optimization result
            OptimizationResult result = OptimizationResult.builder()
                    .tenantId(tenant.getId())
                    .recommendationsGenerated(recommendations.size())
                    .recommendationsExecuted(executedCount)
                    .estimatedMonthlySavings(totalSavings)
                    .success(true)
                    .build();
            
            log.info("Optimization completed for tenant {}: {} recommendations, {} executed, ${} savings",
                    tenant.getId(), recommendations.size(), executedCount, totalSavings);
            
            // Persist the result
            optimizationResultRepository.save(result);
            
            return result;
            
        } catch (Exception e) {
            log.error("Optimization failed for tenant {}: {}", tenant.getId(), e.getMessage());
            
            OptimizationResult failedResult = OptimizationResult.builder()
                    .tenantId(tenant.getId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
            
            // Persist the failure
            optimizationResultRepository.save(failedResult);
            
            return failedResult;
        }
    }
    
    private boolean shouldExecuteAutomatically(Tenant tenant, Recommendation rec) {
        // Check if tenant requires approval for changes
        if (tenant.getSettings().isRequireApprovalForChanges()) {
            return false;
        }
        
        // Check if resource is protected
        if (tenant.getSettings().getProtectedResources().contains(rec.getResourceId())) {
            return false;
        }
        
        // Only execute high-confidence recommendations automatically
        if (rec.getConfidence() < 0.7) {
            return false;
        }
        
        // Don't execute termination actions automatically (safety)
        if ("TERMINATE".equals(rec.getAction()) || "DECOMMISSION".equals(rec.getAction())) {
            return false;
        }
        
        return true;
    }
    
    private boolean executeRecommendation(String tenantId, Recommendation rec) {
        try {
            // Approve and execute the recommendation
            boolean approved = recommendationService.approve(rec.getId());
            if (approved) {
                boolean executed = recommendationService.execute(rec.getId());
                if (executed) {
                    log.info("[AUTONOMOUS] Executed recommendation {} for tenant {}", rec.getId(), tenantId);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to execute recommendation {} for tenant {}: {}", rec.getId(), tenantId, e.getMessage());
            return false;
        }
    }
    
    private void performComprehensiveAnalysis(Tenant tenant) {
        log.info("Performing comprehensive analysis for tenant: {}", tenant.getId());
        
        // Generate detailed savings report
        SavingsReport savings = savingsService.generateReport();
        
        // Check for budget warnings
        if (savings.getCurrentMonthlyCost() > tenant.getSettings().getMonthlyBudgetLimit()) {
            generateBudgetAlert(tenant, savings);
        }
        
        // Audit recent autonomous actions
        List<AuditLog> recentActions = auditService.recentEntities();
        long autonomousActions = recentActions.stream()
                .filter(a -> a.getMode().contains("AUTONOMOUS"))
                .count();
        
        log.info("Comprehensive analysis for tenant {}: ${}/month budget, {} autonomous actions",
                tenant.getId(), savings.getCurrentMonthlyCost(), autonomousActions);
    }
    
    private void performOrphanedResourceCleanup(Tenant tenant) {
        log.info("Performing orphaned resource cleanup for tenant: {}", tenant.getId());
        
        try {
            // Get current metrics to identify orphaned resources
            List<MetricData> metricsList = metricsProvider.fetchMetricsForTenant(tenant.getId());
            MetricData metrics = metricsList.isEmpty() ? null : metricsList.get(0);
            
            // Generate orphan cleanup recommendations
            List<Recommendation> orphanRecs = recommendationService.generateRightsizing(metrics)
                    .stream()
                    .filter(rec -> "ORPHAN_CLEANUP".equals(rec.getAction()))
                    .toList();
            
            // Execute orphan cleanup (safer than regular recommendations)
            int cleanedUp = 0;
            for (Recommendation rec : orphanRecs) {
                if (rec.getConfidence() > 0.8) { // High confidence for cleanup
                    boolean success = executeRecommendation(tenant.getId(), rec);
                    if (success) {
                        cleanedUp++;
                    }
                }
            }
            
            log.info("Orphan cleanup completed for tenant {}: {} resources cleaned up", tenant.getId(), cleanedUp);
            
        } catch (Exception e) {
            log.error("Orphan cleanup failed for tenant {}: {}", tenant.getId(), e.getMessage());
        }
    }
    
    private void generateBudgetAlert(Tenant tenant, SavingsReport savings) {
        log.warn("BUDGET ALERT for tenant {}: Current spend ${} exceeds budget ${}",
                tenant.getId(), savings.getCurrentMonthlyCost(), tenant.getSettings().getMonthlyBudgetLimit());
        
        // Record budget alert in audit log
        AuditLog alert = AuditLog.builder()
                .decisionId("budget-alert-" + tenant.getId())
                .action("BUDGET_EXCEEDED")
                .resourceId("tenant-" + tenant.getId())
                .mode("ALERT")
                .status(com.optibrain.audit.model.AuditStatus.FAILED)
                .explanation(String.format("Current: $%.2f, Budget: $%.2f", 
                        savings.getCurrentMonthlyCost(), tenant.getSettings().getMonthlyBudgetLimit()))
                .score(0)
                .build();
        
        auditService.record(alert);
    }
    
    private void generateOptimizationSummary(List<CompletableFuture<OptimizationResult>> futures) {
        int totalTenants = 0;
        int successfulTenants = 0;
        int totalRecommendations = 0;
        int totalExecutions = 0;
        double totalSavings = 0;
        
        for (CompletableFuture<OptimizationResult> future : futures) {
            try {
                OptimizationResult result = future.get();
                totalTenants++;
                
                if (result.isSuccess()) {
                    successfulTenants++;
                    totalRecommendations += result.getRecommendationsGenerated();
                    totalExecutions += result.getRecommendationsExecuted();
                    totalSavings += result.getEstimatedMonthlySavings();
                }
            } catch (Exception e) {
                log.error("Failed to get optimization result", e);
            }
        }
        
        log.info("=== AUTONOMOUS OPTIMIZATION SUMMARY ===");
        log.info("Tenants processed: {}/{}", successfulTenants, totalTenants);
        log.info("Recommendations generated: {}", totalRecommendations);
        log.info("Recommendations executed: {}", totalExecutions);
        log.info("Estimated monthly savings: ${}", totalSavings);
        log.info("========================================");
    }
}
