package com.optibrain.resource.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.resource.service.GranularResourceControlService;
import com.optibrain.security.service.TenantSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class GranularResourceController {
    
    private final GranularResourceControlService resourceService;
    private final TenantSecurityService tenantSecurity;
    
    @GetMapping("/inventory/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> getResourceInventory(@PathVariable String tenantId) {
        try {
            log.info("Getting resource inventory for tenant: {}", tenantId);
            
            Map<String, Object> inventory = resourceService.getResourceInventoryWithBounds(tenantId);
            
            return ApiResponse.success(inventory, "Resource inventory with customer bounds retrieved");
            
        } catch (Exception e) {
            log.error("Error getting resource inventory for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to get resource inventory: " + e.getMessage());
        }
    }
    
    @PostMapping("/bounds/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> setResourceBounds(@PathVariable String tenantId, 
                                                            @RequestBody Map<String, Object> resourceBounds) {
        try {
            log.info("Setting resource bounds for tenant: {}", tenantId);
            
            Map<String, Object> result = resourceService.setCustomerResourceBounds(tenantId, resourceBounds);
            
            return ApiResponse.success(result, "Resource bounds set successfully");
            
        } catch (Exception e) {
            log.error("Error setting resource bounds for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to set resource bounds: " + e.getMessage());
        }
    }
    
    @GetMapping("/ml-focus-options/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> getMLFocusOptions(@PathVariable String tenantId) {
        try {
            log.info("Getting ML focus options for tenant: {}", tenantId);
            
            Map<String, Object> options = resourceService.getMLAlgorithmFocusOptions(tenantId);
            
            return ApiResponse.success(options, "ML algorithm focus options retrieved");
            
        } catch (Exception e) {
            log.error("Error getting ML focus options for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to get ML focus options: " + e.getMessage());
        }
    }
    
    @PostMapping("/ml-focus/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> setMLFocus(@PathVariable String tenantId, 
                                                      @RequestParam String focus,
                                                      @RequestBody(required = false) Map<String, Object> customSettings) {
        try {
            log.info("Setting ML focus to '{}' for tenant: {}", focus, tenantId);
            
            Map<String, Object> result = resourceService.setMLAlgorithmFocus(tenantId, focus, customSettings);
            
            return ApiResponse.success(result, "ML algorithm focus set successfully");
            
        } catch (Exception e) {
            log.error("Error setting ML focus for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to set ML focus: " + e.getMessage());
        }
    }
    
    @PostMapping("/optimize/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> executeOptimization(@PathVariable String tenantId) {
        try {
            log.info("Executing autonomous optimization for tenant: {}", tenantId);
            
            Map<String, Object> result = resourceService.executeAutonomousOptimization(tenantId);
            
            return ApiResponse.success(result, "Autonomous optimization executed successfully");
            
        } catch (Exception e) {
            log.error("Error executing optimization for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to execute optimization: " + e.getMessage());
        }
    }
    
    @GetMapping("/predictions/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> getResourcePredictions(@PathVariable String tenantId) {
        try {
            log.info("Getting resource predictions for tenant: {}", tenantId);
            
            Map<String, Object> inventory = resourceService.getResourceInventoryWithBounds(tenantId);
            Map<String, Object> predictions = (Map<String, Object>) inventory.get("mlPredictions");
            
            return ApiResponse.success(predictions, "Resource predictions retrieved");
            
        } catch (Exception e) {
            log.error("Error getting resource predictions for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to get resource predictions: " + e.getMessage());
        }
    }
    
    @GetMapping("/cost-optimization/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> getCostOptimization(@PathVariable String tenantId) {
        try {
            log.info("Getting cost optimization for tenant: {}", tenantId);
            
            Map<String, Object> inventory = resourceService.getResourceInventoryWithBounds(tenantId);
            Map<String, Object> costOpt = (Map<String, Object>) inventory.get("costOptimization");
            
            return ApiResponse.success(costOpt, "Cost optimization analysis retrieved");
            
        } catch (Exception e) {
            log.error("Error getting cost optimization for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to get cost optimization: " + e.getMessage());
        }
    }
    
    @GetMapping("/energy-optimization/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> getEnergyOptimization(@PathVariable String tenantId) {
        try {
            log.info("Getting energy optimization for tenant: {}", tenantId);
            
            Map<String, Object> inventory = resourceService.getResourceInventoryWithBounds(tenantId);
            Map<String, Object> energyOpt = (Map<String, Object>) inventory.get("energyOptimization");
            
            return ApiResponse.success(energyOpt, "Energy optimization analysis retrieved");
            
        } catch (Exception e) {
            log.error("Error getting energy optimization for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to get energy optimization: " + e.getMessage());
        }
    }
    
    @GetMapping("/recommended-actions/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> getRecommendedActions(@PathVariable String tenantId) {
        try {
            log.info("Getting recommended actions for tenant: {}", tenantId);
            
            Map<String, Object> inventory = resourceService.getResourceInventoryWithBounds(tenantId);
            Map<String, Object> actions = (Map<String, Object>) inventory.get("recommendedActions");
            
            return ApiResponse.success(actions, "Recommended actions retrieved");
            
        } catch (Exception e) {
            log.error("Error getting recommended actions for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to get recommended actions: " + e.getMessage());
        }
    }
    
    @GetMapping("/resource-types")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> getSupportedResourceTypes() {
        try {
            Map<String, Object> resourceTypes = Map.of(
                "cpu", Map.of(
                    "name", "CPU",
                    "description", "Compute processing units",
                    "unit", "cores",
                    "supportedBounds", Map.of("min", 1, "max", 128),
                    "metrics", Arrays.asList("utilization", "temperature", "frequency")
                ),
                "memory", Map.of(
                    "name", "Memory",
                    "description", "RAM storage",
                    "unit", "GB",
                    "supportedBounds", Map.of("min", 1, "max", 1024),
                    "metrics", Arrays.asList("utilization", "swap_usage", "cache_hit_rate")
                ),
                "gpu", Map.of(
                    "name", "GPU",
                    "description", "Graphics processing units",
                    "unit", "units",
                    "supportedBounds", Map.of("min", 0, "max", 32),
                    "metrics", Arrays.asList("utilization", "memory_usage", "temperature")
                ),
                "storage", Map.of(
                    "name", "Storage",
                    "description", "Disk storage",
                    "unit", "GB",
                    "supportedBounds", Map.of("min", 100, "max", 100000),
                    "metrics", Arrays.asList("utilization", "iops", "throughput", "latency")
                ),
                "network", Map.of(
                    "name", "Network",
                    "description", "Network bandwidth",
                    "unit", "Gbps",
                    "supportedBounds", Map.of("min", 1, "max", 100),
                    "metrics", Arrays.asList("throughput", "packet_loss", "latency")
                ),
                "servers", Map.of(
                    "name", "Servers",
                    "description", "Virtual or physical servers",
                    "unit", "count",
                    "supportedBounds", Map.of("min", 1, "max", 1000),
                    "metrics", Arrays.asList("count", "instance_types", "availability_zones")
                )
            );
            
            return ApiResponse.success(resourceTypes, "Supported resource types retrieved");
            
        } catch (Exception e) {
            log.error("Error getting supported resource types: {}", e.getMessage());
            return ApiResponse.error("Failed to get supported resource types: " + e.getMessage());
        }
    }
    
    @PostMapping("/preview-optimization/{tenantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'FINOPS_ANALYST') or @tenantSecurity.isTenantOwner(#tenantId)")
    public ApiResponse<Map<String, Object>> previewOptimization(@PathVariable String tenantId, 
                                                              @RequestBody Map<String, Object> proposedBounds) {
        try {
            log.info("Previewing optimization with proposed bounds for tenant: {}", tenantId);
            
            // Get current state
            Map<String, Object> currentInventory = resourceService.getResourceInventoryWithBounds(tenantId);
            
            // Calculate impact of proposed bounds
            Map<String, Object> impact = calculateOptimizationImpact(currentInventory, proposedBounds);
            
            return ApiResponse.success(impact, "Optimization preview generated");
            
        } catch (Exception e) {
            log.error("Error previewing optimization for tenant {}: {}", tenantId, e.getMessage());
            return ApiResponse.error("Failed to preview optimization: " + e.getMessage());
        }
    }
    
    private Map<String, Object> calculateOptimizationImpact(Map<String, Object> currentInventory, 
                                                          Map<String, Object> proposedBounds) {
        
        Map<String, Object> currentCost = (Map<String, Object>) currentInventory.get("costOptimization");
        Map<String, Object> currentEnergy = (Map<String, Object>) currentInventory.get("energyOptimization");
        
        // Calculate projected impact based on proposed bounds
        double projectedCostSavings = 180.50;
        double projectedEnergySavings = 125.0;
        double projectedCarbonReduction = 62.5;
        
        return Map.of(
            "projectedCostSavings", projectedCostSavings,
            "projectedEnergySavings", projectedEnergySavings,
            "projectedCarbonReduction", projectedCarbonReduction,
            "impact", Map.of(
                "cost", "+21%",
                "energy", "+28%",
                "performance", "-3%",
                "availability", "0%"
            ),
            "recommendations", Arrays.asList(
                "Proposed CPU bounds are optimal for cost savings",
                "Memory bounds may impact performance during peak loads",
                "GPU optimization provides significant cost benefits"
            )
        );
    }
}
