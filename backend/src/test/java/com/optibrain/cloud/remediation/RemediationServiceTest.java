package com.optibrain.cloud.remediation;

import com.optibrain.cloud.config.CloudProperties;
import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.CloudResource;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.cloud.model.ResourceType;
import com.optibrain.cloud.port.CloudProviderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Planning is what a reviewer reads before approving a change, so its guarantees are
 * pinned here: a protected resource must be reported as blocked, and planning must never
 * mutate the account.
 *
 * <p>These tests drive {@link RemediationService} against a stubbed provider, so they
 * assert the service's own contribution to safety rather than re-testing the adapter's
 * guards, which {@code RemediationSafetyTest} covers end to end.
 */
class RemediationServiceTest {

    private final CloudProviderPort provider = mock(CloudProviderPort.class);
    private final CloudProperties properties = new CloudProperties();

    private RemediationService service(boolean dryRun) {
        properties.setDryRun(dryRun);
        return new RemediationService(provider, properties);
    }

    private RemediationService service() {
        return service(true);
    }

    private CloudResource resource(boolean isProtected) {
        return new CloudResource("vol-1", ResourceType.EBS_VOLUME, "us-east-1",
                "available", "baseline", Map.of(), Map.of("sizeGiB", "100"),
                0.10, 73.0, null, isProtected);
    }

    @Test
    @DisplayName("a destructive action on a protected resource is blocked and explained")
    void blocksDestructiveActionOnProtectedResource() {
        when(provider.find("vol-1")).thenReturn(Optional.of(resource(true)));

        RemediationPlan plan = service().plan(ActionType.DELETE_VOLUME, "vol-1", Map.of());

        assertThat(plan.blocked()).isTrue();
        assertThat(plan.blockedReason()).contains("optibrain:protected");
        assertThat(plan.executable()).isFalse();
    }

    @Test
    @DisplayName("planning never mutates the account")
    void planningDoesNotMutate() {
        when(provider.find("vol-1")).thenReturn(Optional.of(resource(false)));

        service().plan(ActionType.TERMINATE_INSTANCE, "vol-1", Map.of());

        verify(provider, never()).execute(any());
    }

    @Test
    @DisplayName("a protected resource is still blocked only for destructive actions")
    void protectionDoesNotBlockSafeActions() {
        when(provider.find("vol-1")).thenReturn(Optional.of(resource(true)));

        RemediationPlan plan = service(false).plan(ActionType.APPLY_TAGS, "vol-1",
                Map.of("key", "Owner", "value", "team"));

        assertThat(plan.blocked()).isFalse();
        assertThat(plan.protectedResource()).isTrue();
    }

    @Test
    @DisplayName("an unprotected resource is not blocked")
    void doesNotBlockUnprotectedResource() {
        when(provider.find("vol-1")).thenReturn(Optional.of(resource(false)));

        RemediationPlan plan = service(false).plan(ActionType.DELETE_VOLUME, "vol-1", Map.of());

        assertThat(plan.blocked()).isFalse();
    }

    @Test
    @DisplayName("an unknown resource is reported as not found, blocked, and with no saving")
    void unknownResourceIsReportedNotAssumed() {
        when(provider.find("vol-404")).thenReturn(Optional.empty());
        when(provider.region()).thenReturn("us-east-1");

        RemediationPlan plan = service().plan(ActionType.DELETE_VOLUME, "vol-404", Map.of());

        assertThat(plan.currentState()).isEqualTo("not found");
        assertThat(plan.estimatedMonthlySavings()).isNull();
        assertThat(plan.resourceType()).isEqualTo("unknown");
        // Fail closed: a destructive action on a target the guard could not verify is
        // blocked, not reported executable.
        assertThat(plan.blocked()).isTrue();
        assertThat(plan.blockedReason()).contains("could not be found");
        assertThat(plan.executable()).isFalse();
    }

    @Test
    @DisplayName("an unknown non-destructive target is not blocked, only destructive ones")
    void unknownNonDestructiveTargetIsNotBlocked() {
        when(provider.find("vol-404")).thenReturn(Optional.empty());
        when(provider.region()).thenReturn("us-east-1");

        RemediationPlan plan = service().plan(ActionType.START_INSTANCE, "vol-404", Map.of());

        assertThat(plan.blocked()).isFalse();
    }

    @Test
    @DisplayName("cost-increasing actions never report a saving")
    void costIncreasingActionsReportNoSaving() {
        when(provider.find("i-1")).thenReturn(Optional.of(new CloudResource(
                "i-1", ResourceType.EC2_INSTANCE, "us-east-1", "running", "busy",
                Map.of(), Map.of(), null, 100.0, null, false)));

        assertThat(service().plan(ActionType.START_INSTANCE, "i-1", Map.of())
                .estimatedMonthlySavings()).isNull();
        assertThat(service().plan(ActionType.RESIZE_INSTANCE, "i-1",
                Map.of("instanceType", "m5.large")).estimatedMonthlySavings()).isNull();
        assertThat(service().plan(ActionType.APPLY_TAGS, "i-1",
                Map.of("key", "Owner", "value", "team")).estimatedMonthlySavings()).isNull();
    }

    @Test
    @DisplayName("history returns most recent first")
    void historyIsMostRecentFirst() {
        when(provider.execute(any())).thenReturn(ActionResult.rejected(
                ResourceAction.of(ActionType.STOP_INSTANCE, "i-1", true, "test"), "dry-run"));

        RemediationService svc = service();
        svc.execute(ActionType.STOP_INSTANCE, "i-1", Map.of(), true);
        svc.execute(ActionType.START_INSTANCE, "i-2", Map.of(), true);

        java.util.List<RemediationService.ExecutionRecord> history = svc.history();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).resourceId()).isEqualTo("i-2");
        assertThat(history.get(1).resourceId()).isEqualTo("i-1");
    }

    @Test
    @DisplayName("saving is not stated where no cost is attributed")
    void noSavingWithoutCost() {
        CloudResource free = new CloudResource("i-free", ResourceType.EC2_INSTANCE,
                "us-east-1", "running", "free", Map.of(), Map.of(), null, null, null, false);
        when(provider.find("i-free")).thenReturn(Optional.of(free));

        RemediationPlan plan = service().plan(ActionType.STOP_INSTANCE, "i-free", Map.of());

        assertThat(plan.estimatedMonthlySavings()).isNull();
    }

    @Test
    @DisplayName("a destructive plan states the irreversibility rather than burying it")
    void destructivePlanWarnsAboutDataLoss() {
        when(provider.find("vol-1")).thenReturn(Optional.of(resource(false)));

        RemediationPlan plan = service().plan(ActionType.DELETE_VOLUME, "vol-1", Map.of());

        assertThat(plan.steps()).anyMatch(step -> step.contains("cannot be recovered"));
        assertThat(plan.destructive()).isTrue();
    }

    @Test
    @DisplayName("the interlock is reported from configuration, not inferred from the account")
    void dryRunOnlyReflectsConfiguration() {
        when(provider.find("vol-1")).thenReturn(Optional.of(resource(false)));
        when(provider.isSandboxed()).thenReturn(true);

        RemediationPlan sandboxWithDryRunOff =
                service(false).plan(ActionType.DELETE_VOLUME, "vol-1", Map.of());

        // Being a sandbox does not imply dry-run. Conflating the two would tell a reviewer
        // their change is blocked when the interlock is actually disabled.
        assertThat(sandboxWithDryRunOff.dryRunOnly()).isFalse();
        assertThat(sandboxWithDryRunOff.sandboxed()).isTrue();
        assertThat(sandboxWithDryRunOff.executable()).isTrue();
    }

    @Test
    @DisplayName("dry-run mode is reported so a reviewer knows the action cannot apply")
    void dryRunModeIsReported() {
        when(provider.find("vol-1")).thenReturn(Optional.of(resource(false)));

        RemediationPlan plan = service(true).plan(ActionType.DELETE_VOLUME, "vol-1", Map.of());

        assertThat(plan.dryRunOnly()).isTrue();
        assertThat(plan.blocked()).isFalse();
        assertThat(plan.executable()).isFalse();
    }

    @Test
    @DisplayName("execution forwards the caller's dry-run flag and parameters unchanged")
    void executeForwardsDryRunFlag() {
        when(provider.execute(any()))
                .thenReturn(ActionResult.rejected(
                        ResourceAction.of(ActionType.STOP_INSTANCE, "i-1", true, "test"),
                        "dry-run"));

        service().execute(ActionType.STOP_INSTANCE, "i-1", Map.of("why", "test"), true);

        ArgumentCaptor<ResourceAction> action = ArgumentCaptor.forClass(ResourceAction.class);
        verify(provider).execute(action.capture());
        assertThat(action.getValue().dryRun()).isTrue();
        assertThat(action.getValue().parameters()).containsEntry("why", "test");
    }

    @Test
    @DisplayName("null parameters are normalised rather than passed through")
    void executeNormalisesNullParameters() {
        when(provider.execute(any())).thenReturn(ActionResult.rejected(
                ResourceAction.of(ActionType.STOP_INSTANCE, "i-1", true, "test"), "dry-run"));

        service().execute(ActionType.STOP_INSTANCE, "i-1", null, true);

        ArgumentCaptor<ResourceAction> action = ArgumentCaptor.forClass(ResourceAction.class);
        verify(provider).execute(action.capture());
        assertThat(action.getValue().parameters()).isEmpty();
    }
}