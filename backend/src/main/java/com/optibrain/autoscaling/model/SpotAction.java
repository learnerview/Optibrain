package com.optibrain.autoscaling.model;

import com.optibrain.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "spot_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SpotAction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType type;

    private String targetInstanceType;
    private java.time.Instant scheduledTime;

    @Column(nullable = false)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotStatus status;

    @Column(nullable = false)
    private double predictedSavings;
}
