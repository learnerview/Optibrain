package com.optibrain.cloud.aws;

import com.optibrain.cloud.config.CloudMode;
import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.common.context.TenantContext;
import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.service.TenantCredentialService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Credential resolution is the tenant isolation boundary: a tenant must never quietly
 * act on the OptiBrain instance's own account. In AWS mode a tenant with no assumed role
 * is refused; only an explicit operator opt-in can relax that.
 */
class AwsClientFactoryIsolationTest {

    private final CloudProperties properties = new CloudProperties();

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("AWS mode refuses a tenant without a role instead of using the ambient account")
    void awsModeRefusesATenantWithoutARole() {
        properties.setMode(CloudMode.AWS);
        TenantCredentialService tenants = mock(TenantCredentialService.class);
        when(tenants.findTenant("t1")).thenReturn(Optional.of(
                Tenant.builder().id("t1").name("billing").awsRoleArn(null).build()));
        AwsClientFactory factory = new AwsClientFactory(properties, tenants);
        TenantContext.setTenantId("t1");

        assertThatThrownBy(factory::ec2)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("awsRoleArn")
                .hasMessageContaining("allow-ambient-fallback");
    }

    @Test
    @DisplayName("AWS mode refuses a tenant id that does not exist")
    void awsModeRefusesAnUnknownTenantId() {
        properties.setMode(CloudMode.AWS);
        TenantCredentialService tenants = mock(TenantCredentialService.class);
        when(tenants.findTenant("ghost")).thenReturn(Optional.empty());
        AwsClientFactory factory = new AwsClientFactory(properties, tenants);
        TenantContext.setTenantId("ghost");

        assertThatThrownBy(factory::ec2)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no tenant with id");
    }

    @Test
    @DisplayName("sandbox mode never needs tenant identity to build a client")
    void sandboxModeDoesNotNeedTenantCredentials() {
        properties.setMode(CloudMode.SANDBOX);
        AwsClientFactory factory = new AwsClientFactory(properties);

        factory.initialise();

        try (Ec2Client client = factory.ec2()) {
            assertThat(client).isNotNull();
        }
    }
}