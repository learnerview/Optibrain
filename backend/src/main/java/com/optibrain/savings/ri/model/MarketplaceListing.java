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
public class MarketplaceListing {
    private String id;
    private String riId;
    private String sellerTenantId;
    private String instanceType;
    private String region;
    private int remainingTermMonths;
    private double price;
    private double originalPrice;
    private double savingsPercentage;
    private String status; // ACTIVE, SOLD, CANCELLED
    private Instant postedAt;
}
