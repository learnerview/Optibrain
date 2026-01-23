package com.optibrain.cleanup.controller;

import com.optibrain.cleanup.dto.OrphanedResourceResponseDTO;
import com.optibrain.cleanup.service.CleanupService;
import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cleanup")
@RequiredArgsConstructor
public class CleanupController {

    private final CleanupService cleanupService;

    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<List<OrphanedResourceResponseDTO>>> getRecommendations() {
        return ResponseEntity.ok(ApiResponse.success(cleanupService.detectOrphanedResources()));
    }

    @PostMapping("/execute/{resourceId}")
    public ResponseEntity<ApiResponse<String>> executeCleanup(
            @PathVariable String resourceId,
            @RequestParam String resourceType,
            @RequestParam(defaultValue = "true") boolean dryRun) {
        String result = cleanupService.executeCleanup(resourceId, resourceType, dryRun);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
