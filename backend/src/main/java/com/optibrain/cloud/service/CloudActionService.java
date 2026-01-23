package com.optibrain.cloud.service;

import com.optibrain.cloud.adapter.CloudAdapter;
import com.optibrain.cloud.model.CloudAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudActionService {

    private final CloudAdapter cloudAdapter;

    public CloudAction executeAction(String actionType, String resourceId) {
        boolean success = false;
        if ("SCALE_UP".equalsIgnoreCase(actionType)) {
            success = cloudAdapter.scaleUp(resourceId);
        } else if ("SCALE_DOWN".equalsIgnoreCase(actionType)) {
            success = cloudAdapter.scaleDown(resourceId);
        }

        return CloudAction.builder()
                .actionId(UUID.randomUUID().toString())
                .resourceId(resourceId)
                .actionType(actionType)
                .status(success ? "SUCCESS" : "FAILED")
                .build();
    }
}
