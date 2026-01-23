package com.optibrain.autoscaling.service;

import com.optibrain.autoscaling.dto.SpotActionResponseDTO;
import com.optibrain.autoscaling.model.ActionType;
import com.optibrain.autoscaling.model.SpotAction;
import com.optibrain.autoscaling.model.SpotStatus;
import com.optibrain.autoscaling.repository.SpotActionRepository;
import com.optibrain.chat.service.PyBridgeService;
import com.optibrain.common.context.TenantContext;
import com.optibrain.tenant.model.CloudCredentials;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotInstanceService {

    private final PyBridgeService pyBridge;
    private final TenantCredentialService tenantCredentialService;
    private final SpotActionRepository spotActionRepository;

    public List<SpotActionResponseDTO> getActions() {
        return spotActionRepository.findByTenantId(TenantContext.getTenantId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SpotActionResponseDTO> executeSpotMigration(List<SpotActionResponseDTO> requests) {
        String tenantId = TenantContext.getTenantId();
        log.info("Executing spot migration for tenant {}: {} actions", tenantId, requests.size());
        
        List<SpotAction> results = new ArrayList<>();
        for (SpotActionResponseDTO req : requests) {
            SpotAction action = SpotAction.builder()
                    .type(req.type())
                    .resourceId(req.resourceId())
                    .status(SpotStatus.COMPLETED)
                    .predictedSavings(req.predictedSavings())
                    .build();
            action.setTenantId(tenantId);
            spotActionRepository.save(action);
            results.add(action);
        }
        
        return results.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public Map<String, Object> predictInterruptions(String instanceType, String region) {
        return pyBridge.getSpotPrediction(instanceType, region);
    }

    private SpotActionResponseDTO mapToDTO(SpotAction entity) {
        return new SpotActionResponseDTO(
            entity.getType(),
            entity.getResourceId(),
            entity.getStatus(),
            entity.getPredictedSavings(),
            entity.getCreatedAt()
        );
    }
}
