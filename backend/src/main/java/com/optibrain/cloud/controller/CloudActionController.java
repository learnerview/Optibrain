package com.optibrain.cloud.controller;

import com.optibrain.cloud.model.CloudAction;
import com.optibrain.cloud.service.CloudActionService;
import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cloud/actions")
@RequiredArgsConstructor
public class CloudActionController {

    private final CloudActionService cloudActionService;

    @PostMapping("/execute")
    public ApiResponse<CloudAction> executeAction(@RequestParam String action, @RequestParam(defaultValue = "i-12345mock") String resourceId) {
        CloudAction result = cloudActionService.executeAction(action, resourceId);
        return ApiResponse.success(result, "Cloud Action Executed");
    }
}
