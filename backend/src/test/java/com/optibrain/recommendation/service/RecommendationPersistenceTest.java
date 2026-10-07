package com.optibrain.recommendation.service;

import com.optibrain.audit.service.AuditService;
import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.DataStatus;
import com.optibrain.cloud.model.MetricsSeries;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.remediation.RemediationService;
import com.optibrain.common.context.TenantContext;
import com.optibrain.pricing.service.PricingService;
import com.optibrain.recommendation.model.Recommendation;
import com.optibrain.recommendation.repository.RecommendationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The recommendation lifecycle is a decision, so it must survive a restart and the next
 * regeneration. These tests pin that {@code approve}, {@code reject} and {@code execute}
 * write the new status through the repository, and that {@code refresh} regenerates
 * pending rows while leaving decided rows alone.
 */
class RecommendationPersistenceTest {

    private static final String TENANT = "acme";

    private final PricingService pricingService = mock(PricingService.class);
    private final CloudProviderPort cloudProvider = mock(CloudProviderPort.class);
    private final RemediationService remediation = mock(RemediationService.class);
    private final CloudProperties properties = new CloudProperties();
    private final AuditService auditService = mock(AuditService.class);
    private final RecommendationRepository repository = mock(RecommendationRepository.class);

    private final List<Recommendation> saved = new ArrayList<>();

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT);
        when(repository.save(any(Recommendation.class))).thenAnswer(inv -> {
            Recommendation r = inv.getArgument(0);
            saved.add(r);
            return r;
        });
        when(repository.findByIdAndTenantId(anyString(), eq(TENANT)))
                .thenReturn(Optional.empty());
        when(repository.findByTenantIdAndStatus(eq(TENANT), eq("PENDING")))
                .thenAnswer(inv -> new ArrayList<>(saved));
        // Empty telemetry so the rightsizing pass skips every resource it inspects.
        when(cloudProvider.ec2InstanceMetrics(any(), any(), any(), any(), any()))
                .thenReturn(new MetricsSeries("", "CPUUtilization", "Percent",
                        Duration.ofHours(1), List.of(), DataStatus.AVAILABLE));
        service = new RecommendationService(pricingService, cloudProvider, remediation,
                properties, auditService, repository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static Recommendation pending(String id) {
        return Recommendation.builder()
                .id(id)
                .tenantId(TENANT)
                .resourceId("vol-1")
                .action("ORPHAN_CLEANUP")
                .recommendedType("EBS_VOLUME")
                .reason("unattached")
                .monthlySavings(73.0)
                .confidence(0.9)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("approve writes the approved status through the repository")
    void approvePersistsStatus() {
        when(repository.findByIdAndTenantId("rec-1", TENANT))
                .thenReturn(Optional.of(pending("rec-1")));

        boolean approved = service.approve("rec-1");

        assertThat(approved).isTrue();
        ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("a non-pending recommendation cannot be approved and is not saved")
    void approveIgnoresNonPending() {
        Recommendation approved = pending("rec-1");
        approved.setStatus("APPROVED");
        when(repository.findByIdAndTenantId("rec-1", TENANT)).thenReturn(Optional.of(approved));

        boolean result = service.approve("rec-1");

        assertThat(result).isFalse();
        verify(repository, never()).save(any(Recommendation.class));
    }

    @Test
    @DisplayName("reject writes the rejected status through the repository")
    void rejectPersistsStatus() {
        when(repository.findByIdAndTenantId("rec-1", TENANT))
                .thenReturn(Optional.of(pending("rec-1")));

        boolean rejected = service.reject("rec-1");

        assertThat(rejected).isTrue();
        ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("execute persists the executed status for a dry-run success")
    void executePersistsOutcome() {
        Recommendation approved = pending("rec-1");
        approved.setStatus("APPROVED");
        when(repository.findByIdAndTenantId("rec-1", TENANT)).thenReturn(Optional.of(approved));
        when(remediation.execute(any(), eq("vol-1"), any(), anyBoolean()))
                .thenReturn(ActionResult.succeeded(
                        com.optibrain.cloud.model.ResourceAction.of(
                                com.optibrain.cloud.model.ActionType.TERMINATE_INSTANCE,
                                "vol-1", Map.of(), true, "test"),
                        "simulated", 73.0));

        boolean executed = service.execute("rec-1");

        assertThat(executed).isTrue();
        ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("EXECUTED");
        assertThat(captor.getValue().getExecutedAt()).isNotNull();
    }

    @Test
    @DisplayName("refresh regenerates pending rows but never touches decided ones")
    void refreshRegeneratesPendingOnly() {
        properties.setDryRun(true);
        // An unattached, costed volume -> one ORPHAN_CLEANUP row, plus the two advisory
        // commitment rows derived from its attributed spend.
        CloudResource volume = new CloudResource("vol-9", ResourceType.EBS_VOLUME,
                "us-east-1", "available", "vol-9", Map.of(),
                Map.of("attachmentCount", "0"), 0.10, 73.0, null, false);
        when(cloudProvider.discover(any())).thenReturn(List.of(volume));

        List<Map<String, Object>> rows = service.refresh();

        verify(repository).deleteByTenantIdAndStatus(TENANT, "PENDING");
        // Exactly the rows generated this pass, none of which existed before, were saved.
        assertThat(saved).hasSize(3);
        assertThat(saved).allSatisfy(r -> {
            assertThat(r.getTenantId()).isEqualTo(TENANT);
            assertThat(r.getStatus()).isEqualTo("PENDING");
        });
        assertThat(rows).hasSize(3);
    }
}