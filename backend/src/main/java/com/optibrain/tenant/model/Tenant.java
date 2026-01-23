package com.optibrain.tenant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {
    private String id;
    private String name;
    private boolean active;
    private TenantSettings settings;
    private String cloudProvider;
    private String mode;
    private String plan;
    @Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantSettings {
        private boolean autonomousMode;
        private boolean requireApprovalForChanges;
        private List<String> protectedResources;
        private double monthlyBudgetLimit;
    }
}
