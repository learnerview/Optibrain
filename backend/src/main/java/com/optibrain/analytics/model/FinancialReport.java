package com.optibrain.analytics.model;

import com.optibrain.analytics.dto.TopSpenderDTO;
import com.optibrain.common.converter.JsonListConverter;
import com.optibrain.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "financial_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class FinancialReport extends BaseEntity {

    @Column(nullable = false)
    private String period; // MONTHLY, WEEKLY

    @Column(nullable = false)
    private double currentCost;

    private double previousCost;
    private double forecastedCost;

    @Column(nullable = false)
    private double potentialSavings;

    @Convert(converter = JsonListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<TopSpenderDTO> topSpenders;

    @Column(nullable = false)
    private String source; // ML, RULE_ENGINE
}
