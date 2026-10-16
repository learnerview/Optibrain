package com.optibrain.cleanup.service;

import com.optibrain.cleanup.dto.OrphanedResourceResponseDTO;
import com.optibrain.cleanup.model.OrphanedResource;
import com.optibrain.cleanup.repository.OrphanedResourceRepository;
import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.cloud.port.ResourceQuery;
import com.optibrain.common.context.TenantContext;
import com.optibrain.config.repository.TenantConfigurationRepository;
import com.optibrain.config.service.TenantConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * An unknown cost is genuinely unknown, and coercing it to 0.0 would let an orphan read
 * as free. These tests pin that null survives discovery, the repository row, and the DTO.
 * They also pin the inventory's identity rules: a resource rediscovered by a later scan is
 * refreshed rather than duplicated, two tenants may each record the same resource id, and
 * an attachment state that is unknown is never read as unattached.
 */
class CleanupServiceTest {

    private final CloudProviderPort provider = mock(CloudProviderPort.class);
    private final OrphanedResourceRepository repository = mock(OrphanedResourceRepository.class);
    private final TenantConfigurationRepository configurationRepository =
            mock(TenantConfigurationRepository.class);
    private final CleanupService service = new CleanupService(provider,
            new TenantConfigurationService(configurationRepository), repository);

    private final List<OrphanedResource> store = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(provider.provider()).thenReturn("aws");
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    /** Backs the mock repository with a list so a second scan can read what the first wrote. */
    private void wireStore() {
        when(repository.findByTenantId(anyString())).thenAnswer(invocation ->
                store.stream()
                        .filter(row -> invocation.getArgument(0).equals(row.getTenantId()))
                        .toList());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> {
            List<OrphanedResource> rows = invocation.getArgument(0);
            rows.forEach(row -> {
                if (!store.contains(row)) {
                    store.add(row);
                }
            });
            return rows;
        });
    }

    private CloudResource volume(String id, Double monthlyCost) {
        return volume(id, monthlyCost, Map.of("attachmentCount", "0"));
    }

    private CloudResource volume(String id, Double monthlyCost, Map<String, String> specs) {
        return new CloudResource(
                id, ResourceType.EBS_VOLUME, "us-east-1", "available", id,
                Map.of(), specs, 0.0, monthlyCost,
                Instant.parse("2020-01-01T00:00:00Z"), false);
    }

    private OrphanedResource storedRow(String tenantId, String region, String resourceType,
                                       String resourceId) {
        OrphanedResource row = OrphanedResource.builder()
                .resourceId(resourceId)
                .resourceType(resourceType)
                .region(region)
                .provider("aws")
                .estimatedMonthlyCost(1.0)
                .resolved(false)
                .build();
        row.setTenantId(tenantId);
        return row;
    }

    @Test
    @DisplayName("an unknown cost is preserved as null all the way to the response")
    @SuppressWarnings({"unchecked", "rawtypes"})
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

    @Test
    @DisplayName("a resource rediscovered by a second scan is refreshed, not inserted again")
    void rescanOfTheSameResourceDoesNotDuplicateIt() {
        TenantContext.setTenantId("t1");
        wireStore();
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("vol-1", 10.0)));

        service.detectOrphanedResources();
        List<OrphanedResourceResponseDTO> secondScan = service.detectOrphanedResources();

        assertThat(store).hasSize(1);
        assertThat(secondScan).hasSize(1);
        assertThat(secondScan.get(0).resourceId()).isEqualTo("vol-1");
    }

    @Test
    @DisplayName("a rediscovered resource that was closed is reopened")
    void rediscoveredResourceIsReopened() {
        TenantContext.setTenantId("t1");
        wireStore();
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("vol-1", 10.0)));

        service.detectOrphanedResources();
        store.forEach(row -> row.setResolved(true));

        List<OrphanedResourceResponseDTO> secondScan = service.detectOrphanedResources();

        assertThat(secondScan).hasSize(1);
        assertThat(secondScan.get(0).resolved()).isFalse();
        assertThat(store).hasSize(1);
    }

    @Test
    @DisplayName("a row the scan no longer reports is closed")
    void resourceNoLongerReportedIsClosed() {
        TenantContext.setTenantId("t1");
        wireStore();
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("vol-1", 10.0)));
        service.detectOrphanedResources();

        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of());
        List<OrphanedResourceResponseDTO> secondScan = service.detectOrphanedResources();

        assertThat(secondScan).isEmpty();
        assertThat(store).hasSize(1);
        assertThat(store.get(0).isResolved()).isTrue();
    }

    @Test
    @DisplayName("two tenants may each record the same resource id")
    void sameResourceIdInTwoTenantsIsRecordedOnceEach() {
        wireStore();
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("shared-id", 5.0)));

        TenantContext.setTenantId("t1");
        service.detectOrphanedResources();
        TenantContext.setTenantId("t2");
        service.detectOrphanedResources();

        assertThat(store).hasSize(2);
        assertThat(store).extracting(OrphanedResource::getTenantId)
                .containsExactlyInAnyOrder("t1", "t2");
    }

    @Test
    @DisplayName("an explicitly unattached volume is a candidate")
    void volumeWithZeroAttachmentsIsUnattached() {
        TenantContext.setTenantId("t1");
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("vol-0", 1.0, Map.of("attachmentCount", "0"))));

        assertThat(service.detectOrphanedResources()).hasSize(1);
    }

    @Test
    @DisplayName("a volume with attachments is not a candidate")
    void volumeWithAttachmentsIsAttached() {
        TenantContext.setTenantId("t1");
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("vol-2", 1.0, Map.of("attachmentCount", "2"))));

        assertThat(service.detectOrphanedResources()).isEmpty();
    }

    @Test
    @DisplayName("a missing attachment count is unknown, not unattached")
    void volumeWithMissingAttachmentCountIsNotACandidate() {
        TenantContext.setTenantId("t1");
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("vol-unknown", 1.0, Map.of())));

        assertThat(service.detectOrphanedResources()).isEmpty();
    }

    @Test
    @DisplayName("a malformed attachment count is unknown, not unattached")
    void volumeWithMalformedAttachmentCountIsNotACandidate() {
        TenantContext.setTenantId("t1");
        when(provider.discover(ResourceQuery.ofTypes(ResourceType.EBS_VOLUME)))
                .thenReturn(List.of(volume("vol-bad", 1.0, Map.of("attachmentCount", "many"))));

        assertThat(service.detectOrphanedResources()).isEmpty();
    }

    @Test
    @DisplayName("cleaning up a load balancer dispatches the network delete action")
    void loadBalancerCleanupDispatchesTheNetworkDeleteAction() {
        TenantContext.setTenantId("t1");
        when(provider.isSandboxed()).thenReturn(false);
        when(provider.execute(any(ResourceAction.class))).thenReturn(ActionResult.succeeded(
                ResourceAction.of(ActionType.DELETE_NETWORK_RESOURCE, "web-lb", false, "test"),
                "Deleted load balancer web-lb", null));

        String outcome = service.executeCleanup("web-lb", "LOAD_BALANCER", "us-east-1", false);

        assertThat(outcome).startsWith("SUCCESS");
        ArgumentCaptor<ResourceAction> action = ArgumentCaptor.forClass(ResourceAction.class);
        verify(provider).execute(action.capture());
        assertThat(action.getValue().type()).isEqualTo(ActionType.DELETE_NETWORK_RESOURCE);
        assertThat(action.getValue().resourceId()).isEqualTo("web-lb");
    }

    @Test
    @DisplayName("a successful cleanup closes only the row with the same full identity")
    void executeCleanupResolvesOnlyTheMatchingIdentity() {
        TenantContext.setTenantId("t1");
        OrphanedResource usEast = storedRow("t1", "us-east-1", "LOAD_BALANCER", "web-lb");
        OrphanedResource euWest = storedRow("t1", "eu-west-1", "LOAD_BALANCER", "web-lb");
        store.add(usEast);
        store.add(euWest);
        when(repository.findByTenantId("t1")).thenReturn(new ArrayList<>(store));
        when(provider.isSandboxed()).thenReturn(false);
        when(provider.execute(any(ResourceAction.class))).thenReturn(ActionResult.succeeded(
                ResourceAction.of(ActionType.DELETE_NETWORK_RESOURCE, "web-lb", false, "test"),
                "Deleted", null));

        service.executeCleanup("web-lb", "LOAD_BALANCER", "us-east-1", false);

        assertThat(usEast.isResolved()).isTrue();
        assertThat(euWest.isResolved()).isFalse();
        verify(repository).save(usEast);
    }
}
