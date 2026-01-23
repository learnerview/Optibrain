package com.optibrain.savings.ri.controller;

import com.optibrain.savings.ri.model.PurchaseRequest;
import com.optibrain.savings.ri.service.ApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ri/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final ApprovalService approvalService;

    @PostMapping("/{tenantId}")
    public ResponseEntity<PurchaseRequest> submitPurchase(
            @PathVariable String tenantId,
            @RequestBody Map<String, Object> payload) {
            
        String type = (String) payload.getOrDefault("type", "RI");
        String requestedBy = (String) payload.getOrDefault("requestedBy", "USER"); // Should come from auth context
        double cost = Double.parseDouble(String.valueOf(payload.getOrDefault("estimatedCost", "0.0")));
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) payload.get("details");
        
        return ResponseEntity.ok(approvalService.submitRequest(tenantId, type, details, cost, requestedBy));
    }

    @GetMapping("/{tenantId}/pending")
    public ResponseEntity<List<PurchaseRequest>> getPendingRequests(@PathVariable String tenantId) {
        return ResponseEntity.ok(approvalService.getRequests(tenantId)); // Simplified, should filter by status
    }

    @PostMapping("/approve/{requestId}")
    public ResponseEntity<PurchaseRequest> approvePurchase(
            @PathVariable String requestId,
            @RequestParam String approverId) {
        return ResponseEntity.ok(approvalService.approveRequest(requestId, approverId));
    }
}
