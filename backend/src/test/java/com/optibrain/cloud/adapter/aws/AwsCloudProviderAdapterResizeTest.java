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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The resize guard is an adapter-level invariant, so it is tested here rather than
 * through the whole recommendation chain. An instance-type change only makes sense
 * against a stopped instance; simulating or executing one against a live machine must
 * both be refused, and the refusal must happen before any EC2 call.
 */
class AwsCloudProviderAdapterResizeTest {

    @Mock private AwsClientFactory clients;
    @Mock private Ec2Client ec2;
    @Mock private ResourceScanner scanner;

    private CloudProperties properties;
    private AwsCloudProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(clients.ec2()).thenReturn(ec2);
        properties = new CloudProperties();
        properties.setMode(CloudMode.SANDBOX);
        properties.setDryRun(false);
        adapter = new AwsCloudProviderAdapter(clients, properties, List.of(scanner));
    }

    private CloudResource instance(String id, String state) {
        return new CloudResource(id, ResourceType.EC2_INSTANCE, "us-east-1", state, id,
                Map.of(), Map.of(), 1.0, 720.0, null, false);
    }

    private ResourceAction resize(String id, boolean dryRun) {
        return ResourceAction.of(ActionType.RESIZE_INSTANCE, id,
                Map.of("instanceType", "t3.micro"), dryRun, "test");
    }

    @Test
    void resizeRefusesARunningInstanceEvenInDryRun() {
        when(scanner.scan(any())).thenReturn(List.of(instance("i-running", "running")));

        ActionResult result = adapter.execute(resize("i-running", true));

        assertFalse(result.success());
        assertTrue(result.message().contains("must be stopped"),
                "expected stopped-state refusal, got: " + result.message());
        verify(clients, never()).ec2();
    }

    @Test
    void resizeAppliesToAStoppedInstance() {
        when(scanner.scan(any())).thenReturn(List.of(instance("i-stopped", "stopped")));
when(ec2.modifyInstanceAttribute(any(Consumer.class)))
                .thenReturn(ModifyInstanceAttributeResponse.builder().build());

        ActionResult result = adapter.execute(resize("i-stopped", false));

        assertTrue(result.success(), "expected resize success, got: " + result.message());
        verify(ec2).modifyInstanceAttribute(any(Consumer.class));
    }

    @Test
    void startingARunningInstanceStillWorks() {
        when(scanner.scan(any())).thenReturn(List.of(instance("i-running", "running")));
        when(ec2.startInstances(any(Consumer.class))).thenReturn(StartInstancesResponse.builder().build());

        ActionResult result = adapter.execute(
                ResourceAction.of(ActionType.START_INSTANCE, "i-running", false, "test"));

        assertTrue(result.success(), "expected start success, got: " + result.message());
        verify(ec2).startInstances(any(Consumer.class));
    }
}