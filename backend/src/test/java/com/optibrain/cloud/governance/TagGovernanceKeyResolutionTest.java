package com.optibrain.cloud.governance;

import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.port.CloudProviderPort;
import com.optibrain.common.context.TenantContext;
import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Required tag keys are a per-tenant policy. These tests pin that the report uses the
 * current tenant's configured keys, falls back to the platform defaults when the tenant
 * has none, and lets an explicit query parameter override both.
 */
class TagGovernanceKeyResolutionTest {

    private static final String TENANT = "acme";

    private final CloudProviderPort cloudProvider = mock(CloudProviderPort.class);
    private final TenantRepository tenants = mock(TenantRepository.class);

    private TagGovernanceService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT);
        service = new TagGovernanceService(cloudProvider, tenants);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private static CloudResource resource(String id, Map<String, String> tags) {
        return new CloudResource(id, ResourceType.EBS_VOLUME, "us-east-1", "available",
                id, tags, Map.of("sizeGiB", "100"), 0.10, 73.0, null, false);
    }

    @Test
    @DisplayName("uses the tenant's configured required keys when none are supplied")
    void usesTenantConfiguredKeys() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT);
        tenant.setRequiredTagKeys(List.of("AppID"));
        when(tenants.findById(TENANT)).thenReturn(Optional.of(tenant));
        when(cloudProvider.discover(any()))
                .thenReturn(List.of(
                        resource("vol-a", Map.of("AppID", "billing")),
                        resource("vol-b", Map.of())));

        TagCoverageReport report = service.report(null);

        assertThat(report.requiredKeys()).containsExactly("AppID");
        assertThat(report.compliantResources()).isEqualTo(1);
        assertThat(report.resourcesAssessed()).isEqualTo(2);
        assertThat(report.offenders()).extracting("resourceId").containsExactly("vol-b");
    }

    @Test
    @DisplayName("falls back to the platform defaults when the tenant has no keys")
    void fallsBackToDefaultsWithoutTenantKeys() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT);
        when(tenants.findById(TENANT)).thenReturn(Optional.of(tenant));
        when(cloudProvider.discover(any()))
                .thenReturn(List.of(resource("vol-a", Map.of("Environment", "dev"))));

        TagCoverageReport report = service.report(null);

        assertThat(report.requiredKeys()).containsExactly("Environment", "Owner");
        // Environment present, Owner missing -> not compliant.
        assertThat(report.compliantResources()).isZero();
    }

    @Test
    @DisplayName("explicit query keys override the tenant configuration")
    void explicitKeysOverrideTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT);
        tenant.setRequiredTagKeys(List.of("AppID"));
        when(tenants.findById(TENANT)).thenReturn(Optional.of(tenant));
        when(cloudProvider.discover(any()))
                .thenReturn(List.of(resource("vol-a", Map.of("CostCenter", "cc-1"))));

        TagCoverageReport report = service.report(List.of("CostCenter"));

        assertThat(report.requiredKeys()).containsExactly("CostCenter");
        assertThat(report.compliantResources()).isEqualTo(1);
    }
}