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

    /**
     * Estimated monthly cost, or {@code null} when the source does not attribute one.
     * Null is preserved rather than coerced to 0.0: an unknown cost is genuinely unknown,
     * and a 0.0 reads as "free", which would bias decisions toward reclaiming a resource
     * whose true cost is simply not known.
     */
    private Double estimatedMonthlyCost;

    @Builder.Default
    @Column(nullable = false)
    private boolean resolved = false;
}
