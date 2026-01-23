package com.optibrain.cloud.controller;

import com.optibrain.cloud.service.CredentialService;
import com.optibrain.common.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final CredentialService credentialService;

    @PostMapping("/aws/connect")
    public ApiResponse<String> connectAws(@RequestBody AwsConfigDto config) {
        credentialService.connect(config.getAccessKey(), config.getSecretKey(), config.getRegion());
        return ApiResponse.success("Connected to AWS Region: " + config.getRegion(), "Connection Successful");
    }
    
    @PostMapping("/aws/disconnect")
     public ApiResponse<String> disconnectAws() {
        credentialService.disconnect();
        return ApiResponse.success("Disconnected. Reverted to Mock Mode.", "Disconnected");
    }

    @GetMapping("/status")
    public ApiResponse<StatusDto> getStatus() {
        boolean connected = credentialService.isConnected();
        return ApiResponse.success(new StatusDto(
                connected ? "AWS" : "MOCK",
                connected ? credentialService.getRegion() : null,
                connected ? "Connected to AWS Live" : "Running on Simulation Data"
        ), "System Status Retrieved");
    }

    @Data
    public static class AwsConfigDto {
        private String accessKey;
        private String secretKey;
        private String region;
    }
    
    @Data
    @lombok.AllArgsConstructor
    public static class StatusDto {
        private String mode;
        private String region;
        private String message;
    }
}
