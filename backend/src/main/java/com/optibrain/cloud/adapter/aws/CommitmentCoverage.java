package com.optibrain.cloud.adapter.aws;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.Coverage;
import software.amazon.awssdk.services.costexplorer.model.DateInterval;
import software.amazon.awssdk.services.costexplorer.model.GetReservationCoverageRequest;
import software.amazon.awssdk.services.costexplorer.model.GetReservationCoverageResponse;
import software.amazon.awssdk.services.costexplorer.model.GetSavingsPlansUtilizationRequest;
import software.amazon.awssdk.services.costexplorer.model.GetSavingsPlansUtilizationResponse;
import software.amazon.awssdk.services.costexplorer.model.Granularity;
import software.amazon.awssdk.services.costexplorer.model.SavingsPlansUtilization;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Commitment coverage from Cost Explorer.
 *
 * <p>Savings Plans and Reserved Instances discount on-demand rates by 20-70%. Treating
 * on-demand list price as the baseline therefore overstates the benefit for any workload
 * that is already covered, and understates it for one that is not.
 *
 * <p>This measures how much of the recent spend a commitment absorbs, so attributed cost
 * can be reported on a basis comparable to the invoice. Coverage is reported rather than
 * a discount rate applied: per-instance commitment detail is not available at service
 * granularity, and a fabricated per-resource discount would be a worse error than an
 * honest ratio.
 *
 * <p>AWS exposes four Savings Plan families: Compute (up to 66%), EC2 Instance (72%),
 * Database (35%) and SageMaker AI (64%).
 */
@Slf4j
final class CommitmentCoverage {

    private static final int WINDOW_DAYS = 30;

    private volatile double savingsPlansUtilisation;
    private volatile double reservedInstancesCoverage;
    private volatile boolean available;

    /**
     * Reads coverage for the trailing 30 days.
     *
     * <p>A failure marks this unavailable rather than zero. Zero asserts that no
     * commitments exist, which is a different and unsupported claim.
     */
    void refresh(CostExplorerClient ce) {
        try {
            this.savingsPlansUtilisation = readSavingsPlans(ce);
            this.reservedInstancesCoverage = readReservations(ce);
            this.available = true;
            log.debug("Commitment coverage: SP={} RI={}", savingsPlansUtilisation,
                    reservedInstancesCoverage);
        } catch (Exception e) {
            this.available = false;
            log.debug("Commitment coverage unavailable: {}", e.getMessage());
        }
    }

    private double readSavingsPlans(CostExplorerClient ce) {
        GetSavingsPlansUtilizationResponse response = ce.getSavingsPlansUtilization(
                GetSavingsPlansUtilizationRequest.builder()
                        .timePeriod(window())
                        .granularity(Granularity.MONTHLY)
                        .build());

        if (response.total() == null || response.total().utilization() == null) {
            return 0.0;
        }
        SavingsPlansUtilization utilization = response.total().utilization();
        return ratio(utilization.usedCommitment(), utilization.totalCommitment());
    }

    private double readReservations(CostExplorerClient ce) {
        GetReservationCoverageResponse response = ce.getReservationCoverage(
                GetReservationCoverageRequest.builder()
                        .timePeriod(window())
                        .granularity(Granularity.MONTHLY)
                        .build());

        Coverage total = response.total();
        if (total == null || total.coverageCost() == null
                || total.coverageCost().onDemandCost() == null) {
            return 0.0;
        }
        // Cost Explorer reports this as a percentage in the range 0-100, not a
        // fraction. Clamping the parsed value directly would turn every reading at or
        // above one percent into 100% coverage, so it is scaled down before clamping.
        return clamp(parse(total.coverageCost().onDemandCost()) / 100.0);
    }

    private DateInterval window() {
        return DateInterval.builder()
                .start(LocalDate.now().minusDays(WINDOW_DAYS).toString())
                .end(LocalDate.now().toString())
                .build();
    }

    /**
     * The factor by which on-demand cost overstates effective cost.
     *
     * <p>1.0 means nothing is committed, so on-demand is the right basis.
     *
     * @return the multiplier, never less than 1.0
     */
    double effectiveCostFactor() {
        return available ? 1.0 - totalCoverage() : 1.0;
    }

    /** Fraction of recent spend absorbed by a commitment, or 0.0 when unmeasured. */
    double totalCoverage() {
        if (!available) {
            return 0.0;
        }
        return clamp(Math.max(savingsPlansUtilisation, reservedInstancesCoverage));
    }

    /** True when coverage was measured rather than assumed. */
    boolean isAvailable() {
        return available;
    }

    double savingsPlansUtilisation() {
        return round(savingsPlansUtilisation);
    }

    double reservedInstancesCoverage() {
        return round(reservedInstancesCoverage);
    }

    private static double ratio(String used, String total) {
        double denominator = parse(total);
        return denominator <= 0 ? 0.0 : clamp(parse(used) / denominator);
    }

    private static double parse(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return new BigDecimal(value.trim()).doubleValue();
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}