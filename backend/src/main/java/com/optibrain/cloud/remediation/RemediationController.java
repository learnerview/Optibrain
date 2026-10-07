package com.optibrain.cloud.remediation;

import com.optibrain.cloud.model.ActionResult;
import com.optibrain.cloud.model.ActionType;
import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Remediation planning and execution.
 *
 * <p>Two operations with deliberately different guarantees. {@code POST /plan} mutates
 * nothing and returns everything a reviewer needs, including whether policy blocks the
 * action. {@code POST /execute} applies it.
 *
 * <p>Execution is subject to both guards in the provider: the global
 * {@code cloud.dry-run} interlock and the protected-resource check. No request shape can
 * bypass either, because this controller holds no AWS client and reaches the provider
 * through the same port every other service uses.
 */
@RestController
@RequestMapping("/api/remediation")
@RequiredArgsConstructor
public class RemediationController {

    private final RemediationService remediation;

    /** Actions the platform supports, with their blast radius. */
    @PostMapping("/plan")
    public ResponseEntity<ApiResponse<RemediationPlan>> plan(
            @RequestBody RemediationRequest request) {
        if (request == null || request.actionType() == null || request.resourceId() == null
                || request.resourceId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("actionType and resourceId are required"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                remediation.plan(request.actionType(), request.resourceId(), request.parameters()),
                "Remediation planned. No change has been applied."));
    }

    /** Applies an action. Honours {@code dryRun} and the global interlock. */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<ActionResult>> execute(
            @RequestBody RemediationRequest request) {
        if (request == null || request.actionType() == null || request.resourceId() == null
                || request.resourceId().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("actionType and resourceId are required"));
        }
        ActionResult result = remediation.execute(
                request.actionType(), request.resourceId(),
                request.parameters(), request.isDryRun());

        // A rejected action is a client-visible outcome, not a transport failure, so it
        // returns 200 with success=false rather than a 4xx.
        return ResponseEntity.ok(ApiResponse.success(result,
                result.applied() ? "Action applied" : "Action not applied"));
    }

    /** Operations supported, with their blast radius, so the UI can build a plan form. */
    @org.springframework.web.bind.annotation.GetMapping("/actions")
    public ResponseEntity<ApiResponse<java.util.List<RemediationService.ActionSummary>>> actions() {
        return ResponseEntity.ok(ApiResponse.success(remediation.availableActions(),
                "Supported remediation actions"));
    }

    /** Actions requested this session, most recent first. */
    @org.springframework.web.bind.annotation.GetMapping("/history")
    public ResponseEntity<ApiResponse<java.util.List<RemediationService.ExecutionRecord>>> history() {
        return ResponseEntity.ok(ApiResponse.success(remediation.history(),
                "Session remediation history"));
    }

    /**
     * A remediation request.
     *
     * @param actionType operation to perform
     * @param resourceId target resource
     * @param parameters operation-specific arguments
     * @param dryRun boxed so an absent value is distinguishable from an explicit
     *                {@code false}. Absent means dry-run, so applying a change is an
     *                opt-in and never the default.
     */
    public record RemediationRequest(
            ActionType actionType,
            String resourceId,
            Map<String, String> parameters,
            Boolean dryRun
    ) {
        public RemediationRequest {
            parameters = parameters == null ? Map.of()
                    : parameters.entrySet().stream()
                            .filter(e -> e.getKey() != null && e.getValue() != null)
                            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                                    Map.Entry::getKey, Map.Entry::getValue));
        }

        public Map<String, String> parameters() {
            return parameters;
        }

        /**
         * Whether to evaluate without applying.
         *
         * <p>True unless the caller explicitly supplied {@code false}.
         */
        public boolean isDryRun() {
            return !Boolean.FALSE.equals(dryRun);
        }
    }
}