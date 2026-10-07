package com.optibrain.tenant.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.tenant.model.Tenant;
import com.optibrain.tenant.model.TenantUpdateRequest;
import com.optibrain.tenant.service.TenantCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tenant administration.
 *
 * <p>Exposes the full tenant lifecycle, including the create, update, deactivate and
 * reactivate operations that {@code TenantCredentialService} supports.
 *
 * <p>Credentials are not part of this API by design. There is no endpoint that accepts or
 * returns AWS keys; credential resolution belongs to the environment (IAM role,
 * environment variables, shared credentials file) and is applied centrally by
 * {@code AwsClientFactory}.
 */
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantCredentialService tenantService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Tenant>>> list() {
        return ResponseEntity.ok(ApiResponse.success(tenantService.getAllTenants(),
                "Tenants retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Tenant>> get(@PathVariable String id) {
        return tenantService.findTenant(id)
                .map(t -> ResponseEntity.ok(ApiResponse.success(t, "Tenant retrieved")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("No active tenant with id '" + id + "'")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Tenant>> create(@RequestBody Tenant tenant) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(tenantService.createTenant(tenant),
                            "Tenant created"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Tenant>> update(
            @PathVariable String id, @RequestBody TenantUpdateRequest changes) {
        try {
            return ResponseEntity.ok(ApiResponse.success(tenantService.updateTenant(id, changes),
                    "Tenant updated"));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Deactivates rather than deletes.
     *
     * <p>Audit entries, remediation records and cleanup reports all reference the tenant
     * id. Removing the row would leave those dangling, so the tenant is disabled and the
     * history is retained.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        try {
            tenantService.deactivateTenant(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Tenant deactivated"));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<Tenant>> reactivate(@PathVariable String id) {
        try {
            tenantService.reactivateTenant(id);
            return ResponseEntity.ok(ApiResponse.success(tenantService.getTenant(id),
                    "Tenant reactivated"));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}