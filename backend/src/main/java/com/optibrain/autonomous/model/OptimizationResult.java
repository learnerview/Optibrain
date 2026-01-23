package com.optibrain.autonomous.model;

import com.optibrain.common.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "optimization_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OptimizationResult extends BaseEntity {

    private int recommendationsGenerated;
    private int recommendationsExecuted;

    private double estimatedMonthlySavings;

    private boolean success;

    private String errorMessage;
}
