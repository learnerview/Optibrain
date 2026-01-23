package com.optibrain.autoscaling.service;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.service.AuditService;
import com.optibrain.autoscaling.model.SpotAction;
import com.optibrain.autoscaling.repository.SpotActionRepository;
import com.optibrain.cloud.adapter.CloudAdapter;
import com.optibrain.cloud.config.CloudConfig;
import com.optibrain.cloud.remediator.CloudRemediator;
import com.optibrain.decision.model.Decision;
import com.optibrain.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoscalingService {

    private final CloudAdapter cloudAdapter;
    private final CloudRemediator cloudRemediator;
    private final CloudConfig cloudConfig;
    private final AuditService auditService;
    private final PolicyService policyService;
    private final SpotActionRepository spotActionRepository;

    /**
     * Evaluates a decision and autonomously executes the action if safety checks pass
     * Requires DEVOPS_ENGINEER role or higher for execution
     * @param decision The decision containing action, resource details, and metadata
     * @return Updated decision with execution status and details
     */
    @PreAuthorize("hasAnyRole('DEVOPS_ENGINEER', 'ADMIN', 'OWNER')")
    public Decision evaluateAndAct(Decision decision) {
        // Safety checks before execution
        if (!shouldExecute(decision)) {
            decision.setMode("AUTONOMOUS_BLOCKED");
            decision.setReason("Safety guardrail triggered: cooldown or max-change limit");
            recordAudit(decision, "BLOCKED", "Safety guardrail");
            return decision;
        }

        boolean success = false;
        String resourceId = decision.getResourceId();
        
        // Execute action based on decision type with proper resource handling
        switch (decision.getAction()) {
            case "SCALE_UP":
                success = cloudRemediator.executeScaleUp(decision.getRegion(), resourceId);
                break;
            case "SCALE_DOWN":
                success = cloudRemediator.executeScaleDown(decision.getRegion(), resourceId);
                break;
            case "RIGHTSIZE":
                // Extract instanceId and newType from decision details
                String instanceId = extractInstanceId(decision);
                String newType = extractNewInstanceType(decision);
                success = cloudRemediator.applyRightsizing(instanceId, newType);
                break;
            default:
                success = true; // NONE action - no execution needed
        }

        String mode = cloudConfig.isDryRun() ? "DRY_RUN" : "EXECUTED";
        String status = success ? "SUCCESS" : "FAILED";
        decision.setMode(mode);
        recordAudit(decision, mode, status);
        
        // Persist SpotAction
        persistSpotAction(decision, status);
        
        return decision;
    }

    private void persistSpotAction(Decision decision, String status) {
        try {
            com.optibrain.autoscaling.model.ActionType actionType = com.optibrain.autoscaling.model.ActionType.MIGRATE_TO_SPOT;
            if ("FALLBACK".equalsIgnoreCase(decision.getAction())) actionType = com.optibrain.autoscaling.model.ActionType.FALLBACK_TO_ON_DEMAND;
            else if ("BALANCE".equalsIgnoreCase(decision.getAction())) actionType = com.optibrain.autoscaling.model.ActionType.DIVERSITY_BALANCE;
            
            com.optibrain.autoscaling.model.SpotStatus spotStatus = com.optibrain.autoscaling.model.SpotStatus.PENDING;
            if ("SUCCESS".equalsIgnoreCase(status)) spotStatus = com.optibrain.autoscaling.model.SpotStatus.COMPLETED;
            else if ("FAILED".equalsIgnoreCase(status)) spotStatus = com.optibrain.autoscaling.model.SpotStatus.FAILED;

            SpotAction action = SpotAction.builder()
                .tenantId(com.optibrain.common.context.TenantContext.getTenantId())
                .type(actionType)
                .resourceId(decision.getResourceId())
                .targetInstanceType(extractNewInstanceType(decision))
                .scheduledTime(Instant.now())
                .status(spotStatus)
                .predictedSavings(decision.getSavings())
                .build();
            
            spotActionRepository.save(action);
        } catch (Exception e) {
            log.error("Failed to persist spot action: {}", e.getMessage());
        }
    }

    private boolean shouldExecute(Decision decision) {
        // ... (existing implementation)
        // 1. Dry-run always allowed
        if (cloudConfig.isDryRun()) return true;

        // 2. Cooldown: skip if same action in last 10 minutes
        java.time.Instant tenMinutesAgo = java.time.Instant.now().minusSeconds(600);
        boolean recentSameAction = auditService.recent().stream()
                .anyMatch(a -> a.getAction().equals(decision.getAction()) && a.getTimestamp().isAfter(tenMinutesAgo));
        if (recentSameAction) {
            log.info("[SAFETY] Cooldown active for action {}", decision.getAction());
            return false;
        }

        // 3. Max-change per hour: limit to 5 executions per hour
        long executionsLastHour = auditService.countExecutionsInLastHour();
        if (executionsLastHour >= 5) {
            log.info("[SAFETY] Max-change limit reached: {} executions in last hour", executionsLastHour);
            return false;
        }

        // 4. Blast-radius: only act on allowed resources (dynamic allow/deny lists)
        if (!isResourceAllowed(decision.getResourceId(), decision.getAction())) {
            log.info("[SAFETY] Resource {} not allowed for action {} due to blast radius policy", 
                    decision.getResourceId(), decision.getAction());
            return false;
        }

        return true;
    }

    /**
     * Extracts instance ID from decision details or explanation
     * @param decision The decision object containing resource information
     * @return Instance ID for rightsizing operation
     */
    private String extractInstanceId(Decision decision) {
        // Try to get from resourceId first
        if (decision.getResourceId() != null && !decision.getResourceId().isEmpty()) {
            return decision.getResourceId();
        }
        
        // Fallback to explanation parsing
        if (decision.getExplanation() != null && !decision.getExplanation().isEmpty()) {
            for (String explanation : decision.getExplanation()) {
                if (explanation.contains("instance-")) {
                    return explanation.substring(explanation.indexOf("instance-"));
                }
            }
        }
        
        log.warn("[AUTOSCALING] Could not extract instance ID from decision: {}", decision.getDecisionId());
        return "unknown-instance";
    }

    /**
     * Extracts new instance type from decision details or explanation
     * @param decision The decision object containing rightsizing information
     * @return New instance type for rightsizing operation
     */
    private String extractNewInstanceType(Decision decision) {
        // Try to get from explanation
        if (decision.getExplanation() != null && !decision.getExplanation().isEmpty()) {
            for (String explanation : decision.getExplanation()) {
                if (explanation.contains("->") && explanation.contains(".")) {
                    // Extract instance type from "current -> new.type" format
                    String[] parts = explanation.split("->");
                    if (parts.length > 1) {
                        String newType = parts[1].trim();
                        if (newType.contains(".")) {
                            return newType.substring(0, newType.indexOf("(")).trim();
                        }
                        return newType;
                    }
                }
            }
        }
        
        log.warn("[AUTOSCALING] Could not extract new instance type from decision: {}", decision.getDecisionId());
        return "t3.medium"; // Safe fallback
    }

    /**
     * Checks if a resource is allowed for the specified action based on blast radius policies
     * @param resourceId The resource identifier to check
     * @param action The action being performed
     * @return true if the action is allowed for this resource
     */
    private boolean isResourceAllowed(String resourceId, String action) {
        // Get blast radius configuration from policy
        var policy = policyService.getCurrentPolicy();
        
        // Check deny list first (highest priority)
        if (policy.getDeniedResources() != null && policy.getDeniedResources().contains(resourceId)) {
            log.debug("[BLAST_RADIUS] Resource {} is in deny list", resourceId);
            return false;
        }
        
        // Check allow list (if specified)
        if (policy.getAllowedResources() != null && !policy.getAllowedResources().isEmpty()) {
            if (!policy.getAllowedResources().contains(resourceId)) {
                log.debug("[BLAST_RADIUS] Resource {} not in allow list", resourceId);
                return false;
            }
        }
        
        // Check action-specific restrictions
        if (policy.getRestrictedActions() != null && policy.getRestrictedActions().contains(action)) {
            log.debug("[BLAST_RADIUS] Action {} is restricted for resource {}", action, resourceId);
            return false;
        }
        
        // Check resource type restrictions (e.g., no termination on production databases)
        if (isProtectedResourceType(resourceId) && "TERMINATE".equals(action)) {
            log.warn("[BLAST_RADIUS] Protected resource type {} cannot be terminated", resourceId);
            return false;
        }
        
        return true;
    }

    /**
     * Determines if a resource is of a protected type that should not be modified
     * @param resourceId The resource identifier to check
     * @return true if the resource is protected
     */
    private boolean isProtectedResourceType(String resourceId) {
        // Define protected resource patterns
        String[] protectedPatterns = {
            "prod-db", "production-database", "critical-db",
            "master-node", "control-plane", "etcd-"
        };
        
        for (String pattern : protectedPatterns) {
            if (resourceId.toLowerCase().contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    private void recordAudit(Decision decision, String mode, String status) {
        com.optibrain.audit.model.AuditStatus auditStatus = com.optibrain.audit.model.AuditStatus.PENDING;
        if ("SUCCESS".equalsIgnoreCase(status)) auditStatus = com.optibrain.audit.model.AuditStatus.SUCCESS;
        else if ("FAILED".equalsIgnoreCase(status)) auditStatus = com.optibrain.audit.model.AuditStatus.FAILED;

        AuditLog auditLog = AuditLog.builder()
                .decisionId(decision.getDecisionId())
                .action(decision.getAction())
                .resourceId("autoscaling-group-placeholder")
                .region(decision.getRegion())
                .mode(mode)
                .status(auditStatus)
                .reason(decision.getReason())
                .explanation(decision.getExplanation() != null ? String.join("; ", decision.getExplanation()) : "")
                .score(decision.getScore())
                .providerSource(cloudAdapter.getProviderName())
                .build();
        auditService.record(auditLog);
    }
}
