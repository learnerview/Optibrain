package com.optibrain.savings.ri.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsPlan {
    private String id;
    private String tenantId;
    private String provider;
    private String savingsPlanId;
    private String savingsPlanArn;
    private String description;
    private String savingsPlanType; // Compute, EC2Instance
    private String paymentOption; // No Upfront, Partial Upfront, All Upfront
    private String term; // 1yr, 3yr
    private String region;
    private double commitment; // Hourly commitment amount
    private String currency;
    private Instant startTime;
    private Instant endTime;
    private String state;
    private double upfrontPaymentAmount;
    private double recurringPaymentAmount;
}
