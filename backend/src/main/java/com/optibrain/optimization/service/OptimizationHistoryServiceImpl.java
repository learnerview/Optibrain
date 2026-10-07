package com.optibrain.optimization.service;

import com.optibrain.audit.model.AuditLog;
import com.optibrain.audit.model.AuditStatus;
import com.optibrain.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports optimizations that were actually performed.
 *
 * <p>This previously returned five fixed entries in a constructor - including
 * "Resize EC2 instance i-1234 / Applied / $150.50" - so the history page showed a
 * plausible history for an account that had never performed any remediation.
 *
 * <p>History is now read from the audit log, which is written when a remediation is
 * requested. An account that has done nothing shows an empty history, which is the truth.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizationHistoryServiceImpl implements OptimizationHistoryService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public List<Map<String, Object>> getAllOptimizations() {
        List<Map<String, Object>> optimizations = new ArrayList<>();
        for (AuditLog entry : auditLogRepository.findAll()) {
            optimizations.add(toRow(entry));
        }
        optimizations.sort(Comparator.comparing(
                o -> String.valueOf(o.get("timestamp")), Comparator.reverseOrder()));
        return optimizations;
    }

    @Override
    public Map<String, Object> getOptimizationById(String id) {
        return getAllOptimizations().stream()
                .filter(row -> id.equals(row.get("id")))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Map<String, Object>> getOptimizationsByStatus(String status) {
        return getAllOptimizations().stream()
                .filter(row -> status.equalsIgnoreCase(String.valueOf(row.get("status"))))
                .toList();
    }

    private Map<String, Object> toRow(AuditLog entry) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(entry.getId()));
        row.put("action", entry.getAction());
        row.put("resourceId", entry.getResourceId());
        row.put("region", entry.getRegion());
        row.put("status", statusOf(entry.getStatus()));
        row.put("estimatedSavings", entry.getSavings());
        row.put("explanation", entry.getExplanation());
        row.put("score", entry.getScore());
        row.put("timestamp", entry.getCreatedAt() == null
                ? Instant.now().toString() : entry.getCreatedAt().toString());
        return row;
    }

    private String statusOf(AuditStatus status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status) {
            case SUCCESS -> "APPLIED";
            case FAILED -> "FAILED";
            case PENDING -> "PENDING";
        };
    }
}