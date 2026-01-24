package com.optibrain.automation.controller;

import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/automation")
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AutomationController {
    
    private final List<Map<String, Object>> automationRules = new ArrayList<>();
    
    public AutomationController() {
        // Initialize with demo rules
        automationRules.add(createRule("rule-001", "Auto-terminate idle EC2 instances", 
            "Automatically stop EC2 instances with <5% CPU for 7 days", 
            true, "Daily at 2 AM UTC", 320));
        automationRules.add(createRule("rule-002", "Archive old S3 objects", 
            "Move objects older than 90 days to Glacier", 
            true, "Weekly, Sunday 1 AM UTC", 180));
        automationRules.add(createRule("rule-003", "Downsize underutilized RDS", 
            "Automatically downsize RDS instances with <10% usage", 
            false, "Bi-weekly", 250));
    }
    
    private Map<String, Object> createRule(String id, String name, String description, 
                                           boolean enabled, String schedule, int savings) {
        Map<String, Object> rule = new HashMap<>();
        rule.put("id", id);
        rule.put("name", name);
        rule.put("description", description);
        rule.put("enabled", enabled);
        rule.put("schedule", schedule);
        rule.put("savings", savings);
        rule.put("createdAt", LocalDateTime.now().toString());
        return rule;
    }
    
    @GetMapping("/rules")
    public ApiResponse<List<Map<String, Object>>> getAllRules() {
        try {
            log.info("Fetching all automation rules (demo mode)");
            return ApiResponse.success(new ArrayList<>(automationRules), "Rules retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting automation rules: {}", e.getMessage());
            return ApiResponse.error("Failed to get automation rules: " + e.getMessage());
        }
    }
    
    @PostMapping("/rules")
    public ApiResponse<Map<String, Object>> createRule(@RequestBody Map<String, Object> ruleData) {
        try {
            log.info("Creating automation rule (demo mode - persistence only)");
            
            String id = "rule-" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> newRule = new HashMap<>(ruleData);
            newRule.put("id", id);
            newRule.put("createdAt", LocalDateTime.now().toString());
            newRule.putIfAbsent("enabled", false);
            
            automationRules.add(newRule);
            
            log.info("Rule created with ID: {}", id);
            return ApiResponse.success(newRule, "Rule created successfully (execution pending)");
        } catch (Exception e) {
            log.error("Error creating automation rule: {}", e.getMessage());
            return ApiResponse.error("Failed to create rule: " + e.getMessage());
        }
    }
    
    @PatchMapping("/rules/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggleRule(@PathVariable String id) {
        try {
            log.info("Toggling automation rule: {} (demo mode)", id);
            
            for (Map<String, Object> rule : automationRules) {
                if (id.equals(rule.get("id"))) {
                    boolean currentEnabled = (boolean) rule.get("enabled");
                    rule.put("enabled", !currentEnabled);
                    rule.put("updatedAt", LocalDateTime.now().toString());
                    
                    log.info("Rule {} toggled to: {}", id, !currentEnabled);
                    return ApiResponse.success(rule, "Rule toggled successfully");
                }
            }
            
            log.warn("Rule {} not found", id);
            return ApiResponse.error("Rule not found: " + id);
        } catch (Exception e) {
            log.error("Error toggling rule {}: {}", id, e.getMessage());
            return ApiResponse.error("Failed to toggle rule: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable String id) {
        try {
            log.info("Deleting automation rule: {} (demo mode)", id);
            
            boolean removed = automationRules.removeIf(rule -> id.equals(rule.get("id")));
            
            if (removed) {
                log.info("Rule {} deleted successfully", id);
                return ApiResponse.success(null, "Rule deleted successfully");
            } else {
                log.warn("Rule {} not found", id);
                return ApiResponse.error("Rule not found: " + id);
            }
        } catch (Exception e) {
            log.error("Error deleting rule {}: {}", id, e.getMessage());
            return ApiResponse.error("Failed to delete rule: " + e.getMessage());
        }
    }
    
    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getExecutionHistory() {
        try {
            log.info("Fetching automation execution history (demo mode)");
            
            // Demo execution history
            List<Map<String, Object>> history = new ArrayList<>();
            history.add(Map.of(
                "id", "exec-001",
                "ruleId", "rule-001",
                "ruleName", "Auto-terminate idle EC2 instances",
                "status", "SIMULATED",
                "executedAt", LocalDateTime.now().minusHours(2).toString(),
                "message", "Demo Mode - Execution engine in development"
            ));
            
            return ApiResponse.success(history, "Execution history retrieved");
        } catch (Exception e) {
            log.error("Error getting execution history: {}", e.getMessage());
            return ApiResponse.error("Failed to get execution history: " + e.getMessage());
        }
    }
}
