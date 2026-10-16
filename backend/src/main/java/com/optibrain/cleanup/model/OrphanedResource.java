package com.optibrain.cleanup.model;

import com.optibrain.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "orphaned_resources", uniqueConstraints = @UniqueConstraint(
        name = "uq_orphaned_resource_identity",
        columnNames = {"tenant_id", "provider", "region", "resource_type", "resource_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OrphanedResource extends BaseEntity {

    /**
     * Provider-native identifier. Not unique on its own: two tenants may hold resources
     * with the same id, and ids such as a load balancer name repeat across regions. The
     * stable identity is the composite declared on {@link Table} above.
     */
    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "region")
    private String region;

    /** Provider that reported the resource, for example {@code aws}. Part of the identity. */
    @Column(name = "provider")
    private String provider;

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
