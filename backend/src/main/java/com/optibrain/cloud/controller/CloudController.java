package com.optibrain.cloud.controller;

import com.optibrain.cloud.adapter.AwsCloudAdapter;
import com.optibrain.cloud.adapter.CloudAdapter;
import com.optibrain.cloud.model.DiscoveryResult;
import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cloud")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class CloudController {

    private final CloudAdapter cloudAdapter;

    @GetMapping("/discovery")
    public ApiResponse<DiscoveryResult> discoverResources(@RequestParam(defaultValue = "aws") String provider) {
        // Set context for the delegating adapter
        com.optibrain.common.context.TenantContext.setProvider(provider);
        
        try {
            List<String> instances = cloudAdapter.discoverInstances();
            DiscoveryResult result = DiscoveryResult.builder()
                    .provider(provider.toUpperCase())
                    .instances(instances)
                    .build();
            
            return ApiResponse.success(result, "Resources discovered successfully from " + provider.toUpperCase());
        } finally {
            // Clean up provider context if needed, though usually handled by interceptor
        }
    }
}
