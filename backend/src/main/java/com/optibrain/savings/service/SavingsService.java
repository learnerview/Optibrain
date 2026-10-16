package com.optibrain.savings.service;

import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.CostQuery;
import com.optibrain.common.context.TenantContext;
import com.optibrain.recommendation.service.RecommendationService;
import com.optibrain.savings.model.SavingsReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Turns observed spend plus open recommendations into a savings projection.
 *
 * <p>Depends on {@link CloudProviderPort} rather than a metrics-specific abstraction:
 * there is one way to ask the cloud what things cost, and having a second one is how
 * the two could disagree.
 *
 * <p>Commitment coverage is reported alongside the projection. A recommendation priced
 * from on-demand list rates overstates its benefit for an already-committed workload, so
 * the coverage figure is what allows a reader to judge whether the projection is
 * realistic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SavingsService implements SavingsProjectionService {

    private static final double HOURS_PER_MONTH = 730.0;

    private final RecommendationService recommendationService;
    private final CloudProviderPort cloudProvider;
    private final com.optibrain.savings.ri.service.RIService riService;

    public SavingsReport generateReport() {
        var costReport = cloudProvider.costReport(CostQuery.lastDays(30));

        double totalForPeriod = costReport.total() == null ? 0.0 : costReport.total().doubleValue();
        double days = Math.max(1.0, java.time.Duration.between(costReport.start(), costReport.end()).getSeconds() / 86400.0);
        double currentMonthly = totalForPeriod / days * 30.0;
        double currentHourly = currentMonthly / HOURS_PER_MONTH;

        double potentialMonthlySavings = recommendationService.listPending().stream()
                .mapToDouble(r -> Math.max(0, r.getMonthlySavings()))
                .sum();

        // Prefer the adapter's measured Cost Explorer coverage: it is the same number
        // Cost Explorer reports, so the adjustment is traceable rather than assumed.
        Double measuredCoverage = cloudProvider.commitmentCoverage();
        Double riCoverage = measuredCoverage != null ? measuredCoverage : riCoverage();

        // On-demand-priced recommendations overstate their benefit for committed
        // workloads, so the effective projection scales by the uncommitted share. When
        // coverage could not be measured, the raw projection is kept and labelled as
        // unadjusted rather than silently presenting list-rate savings as net.
        boolean adjusted = measuredCoverage != null;
        double effectiveFactor = adjusted ? Math.max(0.0, 1.0 - measuredCoverage) : 1.0;
        double effectiveMonthlySavings = potentialMonthlySavings * effectiveFactor;

        return SavingsReport.builder()
                .generatedAt(LocalDateTime.now())
                .currentHourlyCost(currentHourly)
                .currentMonthlyCost(currentMonthly)
                .potentialMonthlySavings(potentialMonthlySavings)
                .effectiveMonthlySavings(effectiveMonthlySavings)
                .commitmentAdjusted(adjusted)
                .riCoveragePercentage(riCoverage)
                .build();
    }

    /**
     * Commitment coverage from the reservation service, or {@code null} when it cannot be
     * measured. Advisory, so an unavailable source must not fail the whole projection,
     * and it must stay distinguishable from a genuine zero.
     */
    private Double riCoverage() {
        try {
            return riService.calculateCoverage(TenantContext.getTenantId());
        } catch (Exception e) {
            log.debug("RI coverage unavailable: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, Object> getSavingsProjection() {
        SavingsReport report = generateReport();
        Double coverage = report.getRiCoveragePercentage();

        Map<String, Object> projection = new java.util.LinkedHashMap<>();
        projection.put("monthlySavings", report.getPotentialMonthlySavings());
        projection.put("annualSavings", report.getPotentialMonthlySavings() * 12);
        // Effective savings are the projection scaled by the uncommitted share, so a
        // reader sees the realistic benefit after existing commitments. The raw figure
        // stays too, since it is what the recommendations summed to.
        projection.put("effectiveMonthlySavings", report.getEffectiveMonthlySavings() != null
                ? report.getEffectiveMonthlySavings() : report.getPotentialMonthlySavings());
        projection.put("effectiveAnnualSavings", (report.getEffectiveMonthlySavings() != null
                ? report.getEffectiveMonthlySavings() : report.getPotentialMonthlySavings()) * 12);
        projection.put("commitmentAdjusted", Boolean.TRUE.equals(report.getCommitmentAdjusted()));
        projection.put("confidence", (Boolean.TRUE.equals(report.getCommitmentAdjusted())
                ? "Calculated (" + coverage + "% commitment coverage applied)"
                : "Calculated; commitment coverage unavailable, so the headline figure is the on-demand projection"));
        projection.put("assumptions", List.of(
                "Sum of pending recommendations",
                "Recommendation values are priced from on-demand list rates; "
                        + "effectiveMonthlySavings scales that by the share not already "
                        + "covered by a Savings Plan or Reserved Instance",
                "Commitment coverage is measured from Cost Explorer and is unavailable "
                        + "when Cost Explorer cannot be reached"));
        // Reported so a reader can judge how much of the projection is already
        // discounted by an existing commitment. Null means "not measured", which is a
        // different claim from 0%.
        projection.put("commitmentCoveragePercentage", coverage);
        return projection;
    }
}
