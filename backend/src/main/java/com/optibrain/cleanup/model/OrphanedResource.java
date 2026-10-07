package com.optibrain.cleanup.model;

import com.optibrain.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "orphaned_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrphanedResource extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String resourceId;

    @Column(nullable = false)
    private String resourceType;

    private String region;
    private double estimatedMonthlyCost;

    @Builder.Default
    @Column(nullable = false)
    private boolean resolved = false;
}
