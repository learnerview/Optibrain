package com.optibrain.pricing.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstancePricing {
    private String instanceType;
    private String region;
    private double hourlyPrice;
    private double monthlyPrice;
    private String currency;
    private LocalDateTime lastUpdated;
    private String vcpu;
    private String memory;
    private String storage;
    private String networkPerformance;
    
    public InstancePricing(String instanceType, String region, double hourlyPrice) {
        this.instanceType = instanceType;
        this.region = region;
        this.hourlyPrice = hourlyPrice;
        this.monthlyPrice = hourlyPrice * 730; // Approximate monthly hours
        this.currency = "USD";
        this.lastUpdated = LocalDateTime.now();
    }
    
    // Helper methods for backward compatibility
    public double getPricePerHour() {
        return hourlyPrice;
    }
    
    public int getVcpus() {
        // Parse vcpu string like "2" or "2 vCPUs"
        if (vcpu == null) return 2; // default
        try {
            return Integer.parseInt(vcpu.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 2;
        }
    }
    
    public double getMemoryGb() {
        // Parse memory string like "4 GB" or "4.0"
        if (memory == null) return 4.0; // default
        try {
            return Double.parseDouble(memory.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 4.0;
        }
    }
}
