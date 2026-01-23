package com.optibrain.savings.ri.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {
    private String id;
    private String tenantId;
    private String type; // RI, SAVINGS_PLAN
    private Map<String, Object> details; // Specifics of the purchase
    private String status; // PENDING, APPROVED, REJECTED, EXECUTED, FAILED
    private double estimatedCost;
    private String requestedBy;
    private Instant requestedAt;
    private String approvedBy;
    private Instant approvedAt;
    private String rejectionReason;
    private String executionId; // AWS Order ID
}
