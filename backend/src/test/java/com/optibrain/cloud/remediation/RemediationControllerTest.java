package com.optibrain.cloud.remediation;

import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.cloud.model.ResourceAction;
import com.optibrain.common.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The execute endpoint must not wrap a refusal in a success envelope. A refused
 * remediation is still HTTP 200 - refusal is a client-visible outcome, not a transport
 * failure - but the envelope's {@code success} flag has to say so.
 */
class RemediationControllerTest {

    private final RemediationService remediation = mock(RemediationService.class);
    private final RemediationController controller = new RemediationController(remediation);

    private RemediationController.RemediationRequest request() {
        return new RemediationController.RemediationRequest(
                ActionType.STOP_INSTANCE, "i-1", Map.of(), false, "k1", null);
    }

    private void given(ActionResult result) {
        when(remediation.execute(any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(result);
    }

    @Test
    @DisplayName("a non-applied remediation returns a non-success envelope with the result")
    void nonAppliedResultReturnsFailureEnvelope() {
        ActionResult result = ActionResult.rejected(
                ResourceAction.of(ActionType.STOP_INSTANCE, "i-1", false, "test"),
                "Resource is protected");
        given(result);

        ResponseEntity<ApiResponse<ActionResult>> response = controller.execute(request());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Resource is protected");
        assertThat(response.getBody().getData()).isEqualTo(result);
    }

    @Test
    @DisplayName("an applied remediation returns a success envelope")
    void appliedResultReturnsSuccessEnvelope() {
        ActionResult result = ActionResult.succeeded(
                ResourceAction.of(ActionType.STOP_INSTANCE, "i-1", false, "test"), "stopped", 5.0);
        given(result);

        ResponseEntity<ApiResponse<ActionResult>> response = controller.execute(request());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(result);
    }

    @Test
    @DisplayName("a successful dry-run preview returns a success envelope")
    void dryRunPreviewReturnsSuccessEnvelope() {
        ActionResult result = ActionResult.simulated(
                ResourceAction.of(ActionType.STOP_INSTANCE, "i-1", true, "test"), "would stop", 5.0);
        given(result);

        ResponseEntity<ApiResponse<ActionResult>> response = controller.execute(request());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("a refusal reached through a dry-run stays a success envelope")
    void rejectedDryRunStaysSuccessEnvelope() {
        ActionResult result = new ActionResult(false, false, true, ActionType.STOP_INSTANCE,
                "i-1", "Resource is protected", null, "Resource is protected", Instant.now());
        given(result);

        ResponseEntity<ApiResponse<ActionResult>> response = controller.execute(request());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(result);
    }
}
