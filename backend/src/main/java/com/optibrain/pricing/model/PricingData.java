package com.optibrain.pricing.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingData {
    private String instanceType;
    private String region;
    private double hourlyPrice;
    private double monthlyPrice;
    private String currency;
    private LocalDateTime lastUpdated;
    private Map<String, Object> attributes;
    
    public PricingData(String instanceType, String region, double hourlyPrice) {
        this.instanceType = instanceType;
        this.region = region;
        this.hourlyPrice = hourlyPrice;
        this.monthlyPrice = hourlyPrice * 730; // Approximate monthly hours
        this.currency = "USD";
        this.lastUpdated = LocalDateTime.now();
    }
}
