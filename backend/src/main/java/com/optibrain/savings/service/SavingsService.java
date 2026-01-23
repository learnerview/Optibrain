package com.optibrain.savings.service;

import com.optibrain.metrics.provider.MetricsProvider;
import com.optibrain.recommendation.service.RecommendationService;
import com.optibrain.savings.model.SavingsReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SavingsService {

    private final RecommendationService recommendationService;
    private final MetricsProvider metricsProvider;
    private final com.optibrain.savings.ri.service.RIService riService;
    private final com.optibrain.savings.ri.service.SavingsPlanService spService;

    public SavingsReport generateReport() {
        var metrics = metricsProvider.getCurrentMetrics().get(0);
        double currentHourly = metrics.getHourlyCost();
        double currentMonthly = currentHourly * 730;

        // Sum potential savings from pending recommendations
        var pending = recommendationService.listPending();
        double potentialMonthlySavings = pending.stream()
                .mapToDouble(r -> Math.max(0, r.getMonthlySavings()))
                .sum();

        double potentialUpfrontCost = pending.stream()
                .mapToDouble(r -> Math.max(0, r.getUpfrontCost()))
                .sum();

        var potentialPaybackAvg = pending.stream()
                .map(r -> r.getPaybackDays())
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();
        Double potentialAvgPaybackDays = potentialPaybackAvg.isPresent() ? potentialPaybackAvg.getAsDouble() : null;

        var potentialRoiAvg = pending.stream()
                .map(r -> r.getRoiMonthly())
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        Double potentialAvgRoiMonthly = potentialRoiAvg.isPresent() ? potentialRoiAvg.getAsDouble() : null;

        // Sum realized savings from executed recommendations
        var executed = recommendationService.history().stream()
                .filter(r -> "EXECUTED".equals(r.getStatus()))
                .toList();

        double realizedMonthlySavings = executed.stream()
                .mapToDouble(r -> Math.max(0, r.getMonthlySavings()))
                .sum();

        double realizedUpfrontCost = executed.stream()
                .mapToDouble(r -> Math.max(0, r.getUpfrontCost()))
                .sum();

        var realizedPaybackAvg = executed.stream()
                .map(r -> r.getPaybackDays())
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();
        Double realizedAvgPaybackDays = realizedPaybackAvg.isPresent() ? realizedPaybackAvg.getAsDouble() : null;

        var realizedRoiAvg = executed.stream()
                .map(r -> r.getRoiMonthly())
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        Double realizedAvgRoiMonthly = realizedRoiAvg.isPresent() ? realizedRoiAvg.getAsDouble() : null;



        // Fetch RI/SP Data
        String tenantId = metrics.getDimensions() != null ? metrics.getDimensions().get("TenantId") : "default";
        double riCoverage = 0.0;
        double potentialRISavings = 0.0;
        double potentialSPSavings = 0.0;

        try {
            riCoverage = riService.calculateCoverage(tenantId);
            
            var riRecs = riService.getRecommendations(tenantId);
            potentialRISavings = riRecs.stream()
                .mapToDouble(ri -> ri.getRecurringCharges() * 0.2) // Estimate 20% savings vs On-Demand if specific savings not available
                .sum();
                
            var spRecs = spService.getRecommendations(tenantId);
            potentialSPSavings = spRecs.stream()
                .mapToDouble(sp -> sp.getRecurringPaymentAmount())
                .sum();
                
        } catch (Exception e) {
            // Log error but continue with partial report
            // log.warn("Failed to fetch RI/SP data for savings report: {}", e.getMessage());
        }

        return SavingsReport.builder()
                .generatedAt(LocalDateTime.now())
                .currentHourlyCost(currentHourly)
                .currentMonthlyCost(currentMonthly)
                .potentialMonthlySavings(potentialMonthlySavings)
                .realizedMonthlySavings(realizedMonthlySavings)
                .potentialUpfrontCost(potentialUpfrontCost)
                .potentialAvgPaybackDays(potentialAvgPaybackDays)
                .potentialAvgRoiMonthly(potentialAvgRoiMonthly)
                .realizedUpfrontCost(realizedUpfrontCost)
                .realizedAvgPaybackDays(realizedAvgPaybackDays)
                .realizedAvgRoiMonthly(realizedAvgRoiMonthly)
                .totalRecommendations(recommendationService.history().size())
                .pendingRecommendations(pending.size())
                .riCoveragePercentage(riCoverage)
                .potentialRISavingsMonthly(potentialRISavings)
                .potentialSPSavingsMonthly(potentialSPSavings)
                .build();
    }
}
