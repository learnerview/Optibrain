package com.optibrain.autoscaling.controller;

import com.optibrain.autoscaling.dto.SpotActionResponseDTO;
import com.optibrain.autoscaling.service.SpotInstanceService;
import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/autoscaling/spot")
@RequiredArgsConstructor
public class SpotController {

    private final SpotInstanceService spotService;

    @GetMapping("/actions")
    public ResponseEntity<ApiResponse<List<SpotActionResponseDTO>>> getActions() {
        return ResponseEntity.ok(ApiResponse.success(spotService.getActions()));
    }

    @GetMapping("/predict")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrediction(
            @RequestParam String instanceType,
            @RequestParam String region) {
        return ResponseEntity.ok(ApiResponse.success(spotService.predictInterruptions(instanceType, region)));
    }

    @PostMapping("/migrate")
    public ResponseEntity<ApiResponse<List<SpotActionResponseDTO>>> executeMigration(
            @RequestBody List<SpotActionResponseDTO> actions) {
        return ResponseEntity.ok(ApiResponse.success(spotService.executeSpotMigration(actions)));
    }
}
