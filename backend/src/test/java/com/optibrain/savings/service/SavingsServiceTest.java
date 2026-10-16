package com.optibrain.savings.service;

import com.optibrain.cloud.model.CostReport;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.common.context.TenantContext;
import com.optibrain.recommendation.service.RecommendationService;
import com.optibrain.savings.model.SavingsReport;
import com.optibrain.savings.ri.service.RIService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * When the adapter cannot measure commitment coverage, the fallback must ask about the
 * tenant that is actually active. The previous fallback asked about a hardcoded "default"
 * tenant, so a real tenant's commitments were reported as zero coverage.
 */
class SavingsServiceTest {

    private final RecommendationService recommendations = mock(RecommendationService.class);
    private final CloudProviderPort provider = mock(CloudProviderPort.class);
    private final RIService riService = mock(RIService.class);

    private final SavingsService service =
            new SavingsService(recommendations, provider, riService);

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private void noMeasuredCoverage() {
        when(provider.costReport(any())).thenReturn(
                CostReport.empty(Instant.EPOCH, Instant.EPOCH));
        when(recommendations.listPending()).thenReturn(List.of());
        when(provider.commitmentCoverage()).thenReturn(null);
    }

    @Test
    @DisplayName("the fallback computes coverage for the active tenant, not a hardcoded one")
    void fallbackCoverageUsesTheActiveTenant() {
        TenantContext.setTenantId("acme");
        noMeasuredCoverage();
        when(riService.calculateCoverage("acme")).thenReturn(62.5);

        SavingsReport report = service.generateReport();

        assertThat(report.getRiCoveragePercentage()).isEqualTo(62.5);
        assertThat(report.getCommitmentAdjusted()).isFalse();
        verify(riService).calculateCoverage("acme");
    }

    @Test
    @DisplayName("unavailable coverage stays distinct from a genuine zero")
    void unavailableCoverageIsNotReportedAsZero() {
        TenantContext.setTenantId("acme");
        noMeasuredCoverage();
        when(riService.calculateCoverage("acme")).thenReturn(null);

        SavingsReport report = service.generateReport();
        assertThat(report.getRiCoveragePercentage()).isNull();

        Map<String, Object> projection = service.getSavingsProjection();
        assertThat(projection.get("commitmentCoveragePercentage")).isNull();
        assertThat(projection.get("confidence").toString()).contains("unavailable");
    }

    @Test
    @DisplayName("a genuine zero remains a reported zero")
    void genuineZeroIsReportedAsZero() {
        TenantContext.setTenantId("acme");
        noMeasuredCoverage();
        when(riService.calculateCoverage("acme")).thenReturn(0.0);

        SavingsReport report = service.generateReport();
        assertThat(report.getRiCoveragePercentage()).isZero();

        Map<String, Object> projection = service.getSavingsProjection();
        assertThat(projection.get("commitmentCoveragePercentage")).isEqualTo(0.0);
    }
}
