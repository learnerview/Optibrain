package com.optibrain.resource.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Slf4j
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getAllResources() {
        try {
            List<Map<String, Object>> resources = resourceService.getAllResources();
            return ApiResponse.success(resources, "Resources retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting resources: {}", e.getMessage());
            return ApiResponse.error("Failed to get resources: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getResourceById(@PathVariable String id) {
        try {
            Map<String, Object> resource = resourceService.getResourceById(id);
            if (resource == null) {
                return ApiResponse.error("Resource not found");
            }
            return ApiResponse.success(resource, "Resource retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting resource {}: {}", id, e.getMessage());
            return ApiResponse.error("Failed to get resource: " + e.getMessage());
        }
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<Map<String, Object>>> getResourcesByStatus(@PathVariable String status) {
        try {
            List<Map<String, Object>> resources = resourceService.getResourcesByStatus(status);
            return ApiResponse.success(resources, "Resources retrieved successfully");
        } catch (Exception e) {
            log.error("Error getting resources by status {}: {}", status, e.getMessage());
            return ApiResponse.error("Failed to get resources: " + e.getMessage());
        }
    }
}
