package com.optibrain.audit.model;

import com.optibrain.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuditLog extends BaseEntity {

    private String decisionId;
    private String mode; // e.g., MANUAL, AUTONOMOUS
    private String region;
    private String reason;
    private String providerSource;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status;

    @Column(length = 1000)
    private String explanation;

    private double savings;
    private double score;
}
