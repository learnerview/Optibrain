package com.optibrain.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantSecurityService {

    public void validateTenantAccess(String tenantId) {
        // Mock validation
    }
}
