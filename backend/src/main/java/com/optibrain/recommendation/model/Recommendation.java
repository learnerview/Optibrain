package com.optibrain.recommendation.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * A cost optimization recommendation and its approval state.
 *
 * <p>Persisted so a decision survives a restart and is never silently dropped by the
 * next refresh: {@code PENDING} rows are regenerated from live inventory on every read,
 * but {@code APPROVED}, {@code REJECTED} and {@code EXECUTED} rows are the operator's
 * decisions and are kept.
 */
@Entity
@Table(name = "recommendations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    @Id
    @Column(nullable = false, length = 16)
    private String id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    private String resourceId;
    private String currentType;
    private String recommendedType;

    /** RIGHTSIZE, SCALE_UP, SCALE_DOWN, SCHEDULED_STOP, DECOMMISSION, ORPHAN_CLEANUP, RI_OPTIMIZATION, SP_OPTIMIZATION */
    private String action;
    private double currentHourlyCost;
    private double recommendedHourlyCost;
    private double monthlySavings;
    private double upfrontCost;
    private Double roiMonthly;
    private Integer paybackDays;
    private Double confidence;
    private double utilizationCpu;
    private double utilizationMemory;
    private String reason;

    /** Generator-specific evidence, kept so the row does not depend on live state once stored. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recommendation_details",
            joinColumns = @JoinColumn(name = "recommendation_id"))
    @MapKeyColumn(name = "key_name")
    @Column(name = "value_text")
    private Map<String, String> details;

    /** PENDING, APPROVED, REJECTED, EXECUTED, FAILED */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
}