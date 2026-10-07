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
public class ReservedInstance {
    private String id;
    private String tenantId;
    private String provider; // AWS, AZURE, GCP
    private String instanceType;
    private String region;
    private String platform; // Linux, Windows
    private String scope; // AZ or Region
    private String offeringType; // No Upfront, Partial Upfront, All Upfront
    private String term; // 1yr, 3yr
    private Instant startTime;
    private Instant endTime;
    private String state; // active, retired
    private int instanceCount;
    private double fixedPrice;
    private double usagePrice;
    private double recurringCharges;
    private String currency;
}
