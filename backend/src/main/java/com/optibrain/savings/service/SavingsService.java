package com.optibrain.savings.service;

import com.optibrain.metrics.provider.MetricsProvider;
import com.optibrain.recommendation.service.RecommendationService;
import com.optibrain.savings.model.SavingsReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.demo-mode", havingValue = "false", matchIfMissing = true)
public class SavingsService implements SavingsProjectionService {

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

        // Fetch RI/SP Data
        String tenantId = metrics.getDimensions() != null ? metrics.getDimensions().get("TenantId") : "default";
        double riCoverage = 0.0;
        try {
            riCoverage = riService.calculateCoverage(tenantId);
        } catch (Exception e) {}

        return SavingsReport.builder()
                .generatedAt(LocalDateTime.now())
                .currentHourlyCost(currentHourly)
                .currentMonthlyCost(currentMonthly)
                .potentialMonthlySavings(potentialMonthlySavings)
                .riCoveragePercentage(riCoverage)
                .build();
    }

    @Override
    public Map<String, Object> getSavingsProjection() {
        SavingsReport report = generateReport();
        return Map.of(
            "monthlySavings", report.getPotentialMonthlySavings(),
            "annualSavings", report.getPotentialMonthlySavings() * 12,
            "confidence", "Calculated (" + report.getRiCoveragePercentage() + "% RI Coverage)",
            "assumptions", List.of("Based on pending recommendations", "Includes RI/SP coverage analysis")
        );
    }
}
