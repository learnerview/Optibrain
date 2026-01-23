package com.optibrain.savings.ri.service;

import com.optibrain.savings.ri.model.PurchaseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final RIService riService;
    
    // In-memory storage for purchase requests (mocking DB for now)
    private final Map<String, PurchaseRequest> requestStore = new ConcurrentHashMap<>();

    public PurchaseRequest submitRequest(String tenantId, String type, Map<String, Object> details, double estimatedCost, String requestedBy) {
        String id = UUID.randomUUID().toString();
        
        PurchaseRequest request = PurchaseRequest.builder()
                .id(id)
                .tenantId(tenantId)
                .type(type)
                .details(details)
                .status("PENDING")
                .estimatedCost(estimatedCost)
                .requestedBy(requestedBy)
                .requestedAt(Instant.now())
                .build();
        
        // Auto-approval logic
        if (shouldAutoApprove(estimatedCost)) {
            log.info("Auto-approving purchase request {} for tenant {}", id, tenantId);
            approveRequest(id, "SYSTEM_AUTO_APPROVE");
        } else {
            log.info("Purchase request {} submitted, pending approval. Cost: {}", id, estimatedCost);
        }
        
        requestStore.put(id, request);
        return request;
    }

    public List<PurchaseRequest> getRequests(String tenantId) {
        return requestStore.values().stream()
                .filter(r -> r.getTenantId().equals(tenantId))
                .toList();
    }

    public PurchaseRequest approveRequest(String requestId, String approverId) {
        PurchaseRequest request = requestStore.get(requestId);
        if (request == null) {
            throw new RuntimeException("Request not found: " + requestId);
        }
        
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request is not in PENDING state");
        }
        
        log.info("Approving request {} by {}", requestId, approverId);
        request.setStatus("APPROVED");
        request.setApprovedBy(approverId);
        request.setApprovedAt(Instant.now());
        
        // Execute the purchase
        try {
            String executionId = riService.executePurchase(request);
            request.setStatus("EXECUTED");
            request.setExecutionId(executionId);
            log.info("Purchase executed successfully: {}", executionId);
        } catch (Exception e) {
            log.error("Failed to execute purchase: {}", e.getMessage());
            request.setStatus("FAILED");
            request.setRejectionReason("Execution failed: " + e.getMessage());
        }
        
        requestStore.put(requestId, request);
        return request;
    }
    
    public PurchaseRequest rejectRequest(String requestId, String rejectorId, String reason) {
        PurchaseRequest request = requestStore.get(requestId);
        if (request == null) {
            throw new RuntimeException("Request not found");
        }
        
        request.setStatus("REJECTED");
        request.setRejectionReason(reason);
        requestStore.put(requestId, request);
        
        log.info("Request {} rejected by {}: {}", requestId, rejectorId, reason);
        return request;
    }
    
    private boolean shouldAutoApprove(double cost) {
        // Auto-approve if cost is under $1000 threshold
        return cost < 1000.0;
    }
}
