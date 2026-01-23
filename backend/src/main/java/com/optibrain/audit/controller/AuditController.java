package com.optibrain.audit.controller;

import com.optibrain.audit.dto.AuditLogResponseDTO;
import com.optibrain.audit.service.AuditService;
import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDTO>>> getRecentLogs() {
        return ResponseEntity.ok(ApiResponse.success(auditService.recent()));
    }
    
    @GetMapping("/logs/paged")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDTO>>> getPagedLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getLogs(page, size)));
    }
}
