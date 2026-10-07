package com.optibrain.cleanup.service;

import com.optibrain.cleanup.dto.OrphanedResourceResponseDTO;
import com.optibrain.cleanup.model.OrphanedResource;
import com.optibrain.cleanup.repository.OrphanedResourceRepository;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.ResourceQuery;
import com.optibrain.common.context.TenantContext;
import com.optibrain.config.service.TenantConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An unknown cost is genuinely unknown, and coercing it to 0.0 would let an orphan read
 * as free. These tests pin that null survives discovery, the repository row, and the DTO.
 */
class CleanupServiceTest {

    private final CloudProviderPort provider = mock(CloudProviderPort.class);
    private final OrphanedResourceRepository repository = mock(OrphanedResourceRepository.class);
    private final CleanupService service =
            new CleanupService(provider, new TenantConfigurationService(), repository);

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private CloudResource volume(String id, Double monthlyCost) {
        return new CloudResource(
                id, ResourceType.EBS_VOLUME, "us-east-1", "available", id,
                Map.of(), Map.of("attachmentCount", "0"), 0.0, monthlyCost,
                Instant.parse("2020-01-01T00:00:00Z"), false);
    }

    @Test
    @DisplayName("an unknown cost is preserved as null all the way to the response")
    void unknownCostIsPreservedAsNullInsteadOfCoercedToZero() {
        TenantContext.setTenantId("t1");
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("vol-costless", null)));

        List<OrphanedResourceResponseDTO> orphans = service.detectOrphanedResources();

        assertThat(orphans).hasSize(1);
        assertThat(orphans.get(0).estimatedMonthlyCost()).isNull();

        ArgumentCaptor<List<OrphanedResource>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(1);
        assertThat(saved.getValue().get(0).getEstimatedMonthlyCost()).isNull();
    }

    @Test
    @DisplayName("a known cost still reaches the response")
    void knownCostIsPreservedOnTheOrphan() {
        TenantContext.setTenantId("t1");
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("vol-priced", 37.44)));

        List<OrphanedResourceResponseDTO> orphans = service.detectOrphanedResources();

        assertThat(orphans).hasSize(1);
        assertThat(orphans.get(0).estimatedMonthlyCost()).isEqualTo(37.44);
    }
}