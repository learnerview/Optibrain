package com.optibrain.cloud.adapter.aws;

import com.optibrain.cloud.aws.AwsClientFactory;
import com.optibrain.cloud.config.CloudMode;
import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.port.ResourceScanner;
import com.optibrain.common.context.TenantContext;
import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.service.TenantCredentialService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DeleteLoadBalancerRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The adapter is the only place a mutation reaches AWS, so both guards are tested here:
 * load balancer deletion must reach the ELBv2 API with the ARN the inventory carries, and
 * a destructive action on a resource a tenant has listed as protected must be refused even
 * when the resource carries no protection tag.
 */
class AwsCloudProviderAdapterMutationGuardTest {

    private static final String ARN =
            "arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/web-lb/abc";

    @Mock private AwsClientFactory clients;
    @Mock private Ec2Client ec2;
    @Mock private ElasticLoadBalancingV2Client elb;
    @Mock private ResourceScanner scanner;
    @Mock private TenantCredentialService tenants;

    private AwsCloudProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(clients.ec2()).thenReturn(ec2);
        when(clients.elasticLoadBalancing()).thenReturn(elb);
        CloudProperties properties = new CloudProperties();
        properties.setMode(CloudMode.AWS);
        properties.setDryRun(false);
        adapter = new AwsCloudProviderAdapter(clients, properties, tenants, List.of(scanner));
        TenantContext.setTenantId("acme");
        when(tenants.findTenant(anyString())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private CloudResource loadBalancer(String id, Map<String, String> specs) {
        return new CloudResource(id, ResourceType.LOAD_BALANCER, "us-east-1", "active", id,
                Map.of(), specs, null, null, null, false);
    }

    private CloudResource volume(String id, boolean protectedByTag) {
        return new CloudResource(id, ResourceType.EBS_VOLUME, "us-east-1", "available", id,
                Map.of(), Map.of(), 1.0, 10.0, null, protectedByTag);
    }

    @Test
    @DisplayName("deleting an idle load balancer calls the ELBv2 API with the resource ARN")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void loadBalancerDeletionUsesTheElbApi() {
        when(scanner.scan(any())).thenReturn(List.of(loadBalancer("web-lb", Map.of("arn", ARN))));

        ActionResult result = adapter.execute(ResourceAction.of(
                ActionType.DELETE_NETWORK_RESOURCE, "web-lb", false, "orphan cleanup"));

        assertThat(result.success()).isTrue();
        assertThat(result.applied()).isTrue();
        ArgumentCaptor<Consumer<DeleteLoadBalancerRequest.Builder>> captor =
                ArgumentCaptor.forClass(Consumer.class);
        verify(elb).deleteLoadBalancer(captor.capture());
        DeleteLoadBalancerRequest.Builder builder = DeleteLoadBalancerRequest.builder();
        captor.getValue().accept(builder);
        assertThat(builder.build().loadBalancerArn()).isEqualTo(ARN);
        verify(ec2, never()).deleteVolume(any(Consumer.class));
    }

    @Test
    @DisplayName("a load balancer with no resolvable ARN is refused rather than deleted by guesswork")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void loadBalancerWithoutArnIsRefused() {
        when(scanner.scan(any())).thenReturn(List.of(loadBalancer("web-lb", Map.of())));

        ActionResult result = adapter.execute(ResourceAction.of(
                ActionType.DELETE_NETWORK_RESOURCE, "web-lb", false, "orphan cleanup"));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("ARN");
        verify(elb, never()).deleteLoadBalancer(any(Consumer.class));
    }

    @Test
    @DisplayName("a tenant-protected resource is refused even without the protection tag")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void tenantProtectedResourceIsRefused() {
        when(scanner.scan(any())).thenReturn(List.of(volume("vol-1", false)));
        Tenant tenant = Tenant.builder().id("acme").name("Acme").build();
        tenant.setProtectedResources(List.of("vol-1"));
        when(tenants.findTenant("acme")).thenReturn(Optional.of(tenant));

        ActionResult result = adapter.execute(ResourceAction.of(
                ActionType.DELETE_VOLUME, "vol-1", false, "test"));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("protected-resource list");
        verify(ec2, never()).deleteVolume(any(Consumer.class));
    }

    @Test
    @DisplayName("a resource not listed as protected is still deletable")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void unlistedResourceIsStillDeletable() {
        when(scanner.scan(any())).thenReturn(List.of(volume("vol-2", false)));
        Tenant tenant = Tenant.builder().id("acme").name("Acme").build();
        tenant.setProtectedResources(List.of("vol-1"));
        when(tenants.findTenant("acme")).thenReturn(Optional.of(tenant));

        ActionResult result = adapter.execute(ResourceAction.of(
                ActionType.DELETE_VOLUME, "vol-2", false, "test"));

        assertThat(result.success()).isTrue();
        verify(ec2).deleteVolume(any(Consumer.class));
    }

    @Test
    @DisplayName("the protection tag still blocks a destructive action")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void taggedResourceIsRefused() {
        when(scanner.scan(any())).thenReturn(List.of(volume("vol-tagged", true)));

        ActionResult result = adapter.execute(ResourceAction.of(
                ActionType.DELETE_VOLUME, "vol-tagged", false, "test"));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("optibrain:protected");
        verify(ec2, never()).deleteVolume(any(Consumer.class));
    }
}
