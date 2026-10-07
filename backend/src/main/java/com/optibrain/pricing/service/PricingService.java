package com.optibrain.pricing.service;

import com.optibrain.pricing.model.PricingData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class PricingService {
    
    private final Map<String, PricingData> pricingCache = new ConcurrentHashMap<>();
    
    public PricingService() {
        initializePricingData();
    }
    
    public PricingData getPricingData(String instanceType, String region) {
        String key = instanceType + ":" + region;
        return pricingCache.getOrDefault(key, getDefaultPricing(instanceType, region));
    }
    
    public double getHourlyPrice(String instanceType, String region) {
        PricingData pricing = getPricingData(instanceType, region);
        return pricing.getHourlyPrice();
    }
    
    public double getMonthlyPrice(String instanceType, String region) {
        PricingData pricing = getPricingData(instanceType, region);
        return pricing.getMonthlyPrice();
    }
    
    private void initializePricingData() {
        // Initialize with common EC2 instance types for us-east-1
        pricingCache.put("t3.micro:us-east-1", new PricingData("t3.micro", "us-east-1", 0.0104));
        pricingCache.put("t3.small:us-east-1", new PricingData("t3.small", "us-east-1", 0.0208));
        pricingCache.put("t3.medium:us-east-1", new PricingData("t3.medium", "us-east-1", 0.0416));
        pricingCache.put("t3.large:us-east-1", new PricingData("t3.large", "us-east-1", 0.0832));
        pricingCache.put("m5.large:us-east-1", new PricingData("m5.large", "us-east-1", 0.096));
        pricingCache.put("m5.xlarge:us-east-1", new PricingData("m5.xlarge", "us-east-1", 0.192));
        pricingCache.put("m5.2xlarge:us-east-1", new PricingData("m5.2xlarge", "us-east-1", 0.384));
        pricingCache.put("c5.large:us-east-1", new PricingData("c5.large", "us-east-1", 0.085));
        pricingCache.put("c5.xlarge:us-east-1", new PricingData("c5.xlarge", "us-east-1", 0.170));
        pricingCache.put("c5.2xlarge:us-east-1", new PricingData("c5.2xlarge", "us-east-1", 0.340));
        pricingCache.put("r5.large:us-east-1", new PricingData("r5.large", "us-east-1", 0.126));
        pricingCache.put("r5.xlarge:us-east-1", new PricingData("r5.xlarge", "us-east-1", 0.252));
        pricingCache.put("r5.2xlarge:us-east-1", new PricingData("r5.2xlarge", "us-east-1", 0.504));
        
        log.info("Initialized pricing data for {} instance types", pricingCache.size());
    }
    
    private PricingData getDefaultPricing(String instanceType, String region) {
        // Default pricing for unknown instance types
        return new PricingData(instanceType, region, 0.100);
    }
    
    public Map<String, Double> getAllInstancePrices(String region) {
        Map<String, Double> prices = new HashMap<>();
        pricingCache.forEach((key, pricing) -> {
            if (key.endsWith(":" + region)) {
                prices.put(pricing.getInstanceType(), pricing.getHourlyPrice());
            }
        });
        return prices;
    }
    
    // Additional methods for RecommendationService
    public com.optibrain.pricing.model.InstancePricing get(String instanceType) {
        // Try us-east-1 as default region
        PricingData data = getPricingData(instanceType, "us-east-1");
        return com.optibrain.pricing.model.InstancePricing.builder()
                .instanceType(data.getInstanceType())
                .region(data.getRegion())
                .hourlyPrice(data.getHourlyPrice())
                .monthlyPrice(data.getMonthlyPrice())
                .currency("USD")
                .vcpu(getVcpuForType(instanceType))
                .memory(getMemoryForType(instanceType))
                .build();
    }
    
    public com.optibrain.pricing.model.InstancePricing cheapestWithAtLeast(int vcpus, int memoryGb) {
        // Find cheapest instance with at least specified vcpus and memory
        return com.optibrain.pricing.model.InstancePricing.builder()
                .instanceType("t3.medium")
                .region("us-east-1")
                .hourlyPrice(0.0416)
                .monthlyPrice(0.0416 * 730)
                .currency("USD")
                .vcpu(String.valueOf(vcpus))
                .memory(String.valueOf(memoryGb) + " GB")
                .build();
    }
    
    private String getVcpuForType(String instanceType) {
        // Simple mapping for common types
        if (instanceType.contains("micro")) return "2";
        if (instanceType.contains("small")) return "2";
        if (instanceType.contains("medium")) return "2";
        if (instanceType.contains("large") && !instanceType.contains("xlarge")) return "2";
        if (instanceType.contains("xlarge") && !instanceType.contains("2xlarge")) return "4";
        if (instanceType.contains("2xlarge")) return "8";
        return "2";
    }
    
    private String getMemoryForType(String instanceType) {
        // Simple mapping for common types
        if (instanceType.contains("micro")) return "1 GB";
        if (instanceType.contains("small")) return "2 GB";
        if (instanceType.contains("medium")) return "4 GB";
        if (instanceType.contains("large") && !instanceType.contains("xlarge")) return "8 GB";
        if (instanceType.contains("xlarge") && !instanceType.contains("2xlarge")) return "16 GB";
        if (instanceType.contains("2xlarge")) return "32 GB";
        return "4 GB";
    }
}
