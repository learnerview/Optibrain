package com.optibrain.pricing.service;

import com.optibrain.pricing.model.InstancePricing;
import com.optibrain.pricing.model.PricingData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pricing for EC2 instance types by region.
 *
 * <p>Prices are looked up from a bounded in-memory cache keyed by {@code instanceType:region}.
 * Unknown type/region combinations return {@code null} rather than a fabricated price, so that
 * callers cannot mistake a synthetic default for a verified rate.
 */
@Service
@Slf4j
public class PricingService {

    private final Map<String, PricingData> pricingCache = new ConcurrentHashMap<>();

    /** Immutable instance-type specification: vCPU count and memory in GiB. */
    private record InstanceSpec(int vcpu, int memoryGb) {}

    /** Verified EC2 instance specifications (us-east-1 on-demand list pricing). */
    private static final Map<String, InstanceSpec> INSTANCE_SPECS = new HashMap<>();

    static {
        INSTANCE_SPECS.put("t3.micro", new InstanceSpec(2, 1));
        INSTANCE_SPECS.put("t3.small", new InstanceSpec(2, 2));
        INSTANCE_SPECS.put("t3.medium", new InstanceSpec(2, 4));
        INSTANCE_SPECS.put("t3.large", new InstanceSpec(2, 8));
        INSTANCE_SPECS.put("t3.xlarge", new InstanceSpec(4, 16));
        INSTANCE_SPECS.put("t3.2xlarge", new InstanceSpec(8, 32));
        INSTANCE_SPECS.put("m5.large", new InstanceSpec(2, 8));
        INSTANCE_SPECS.put("m5.xlarge", new InstanceSpec(4, 16));
        INSTANCE_SPECS.put("m5.2xlarge", new InstanceSpec(8, 32));
        INSTANCE_SPECS.put("m5.4xlarge", new InstanceSpec(16, 64));
        INSTANCE_SPECS.put("m5.8xlarge", new InstanceSpec(32, 128));
        INSTANCE_SPECS.put("c5.large", new InstanceSpec(2, 4));
        INSTANCE_SPECS.put("c5.xlarge", new InstanceSpec(4, 8));
        INSTANCE_SPECS.put("c5.2xlarge", new InstanceSpec(8, 16));
        INSTANCE_SPECS.put("c5.4xlarge", new InstanceSpec(16, 32));
        INSTANCE_SPECS.put("c5.9xlarge", new InstanceSpec(36, 72));
        INSTANCE_SPECS.put("r5.large", new InstanceSpec(2, 16));
        INSTANCE_SPECS.put("r5.xlarge", new InstanceSpec(4, 32));
        INSTANCE_SPECS.put("r5.2xlarge", new InstanceSpec(8, 64));
        INSTANCE_SPECS.put("r5.4xlarge", new InstanceSpec(16, 128));
    }

    public PricingService() {
        initializePricingData();
    }

    /**
     * Look up pricing for an instance type and region.
     *
     * @return the pricing data, or {@code null} when the type/region is not in the cache.
     *         Callers must treat {@code null} as "price unknown" and must not fabricate a price.
     */
    public PricingData getPricingData(String instanceType, String region) {
        String key = instanceType + ":" + region;
        return pricingCache.get(key);
    }

    public Double getHourlyPrice(String instanceType, String region) {
        PricingData pricing = getPricingData(instanceType, region);
        return pricing == null ? null : pricing.getHourlyPrice();
    }

    public Double getMonthlyPrice(String instanceType, String region) {
        PricingData pricing = getPricingData(instanceType, region);
        return pricing == null ? null : pricing.getMonthlyPrice();
    }

    private void initializePricingData() {
        // Approximate us-east-1 on-demand hourly prices (USD).
        put("t3.micro", "us-east-1", 0.0104);
        put("t3.small", "us-east-1", 0.0208);
        put("t3.medium", "us-east-1", 0.0416);
        put("t3.large", "us-east-1", 0.0832);
        put("t3.xlarge", "us-east-1", 0.1664);
        put("t3.2xlarge", "us-east-1", 0.3328);
        put("m5.large", "us-east-1", 0.096);
        put("m5.xlarge", "us-east-1", 0.192);
        put("m5.2xlarge", "us-east-1", 0.384);
        put("m5.4xlarge", "us-east-1", 0.768);
        put("m5.8xlarge", "us-east-1", 1.536);
        put("c5.large", "us-east-1", 0.085);
        put("c5.xlarge", "us-east-1", 0.170);
        put("c5.2xlarge", "us-east-1", 0.340);
        put("c5.4xlarge", "us-east-1", 0.680);
        put("c5.9xlarge", "us-east-1", 1.530);
        put("r5.large", "us-east-1", 0.126);
        put("r5.xlarge", "us-east-1", 0.252);
        put("r5.2xlarge", "us-east-1", 0.504);
        put("r5.4xlarge", "us-east-1", 1.008);

        log.info("Initialized pricing data for {} instance types", pricingCache.size());
    }

    private void put(String instanceType, String region, double hourlyPrice) {
        String key = instanceType + ":" + region;
        double monthly = round(hourlyPrice * 730.0, 2);
        pricingCache.put(key, new PricingData(instanceType, region, hourlyPrice));
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
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

    // ------------------------------------------------------------------
    // RecommendationService integration
    // ------------------------------------------------------------------

    /**
     * Look up pricing for an instance type in a specific region.
     *
     * @param instanceType the EC2 instance type
     * @param region       the AWS region (e.g. {@code us-east-1})
     * @return the pricing model, or {@code null} when the type/region is unknown
     */
    public com.optibrain.pricing.model.InstancePricing get(String instanceType, String region) {
        PricingData data = getPricingData(instanceType, region);
        if (data == null) {
            return null;
        }
        InstanceSpec spec = INSTANCE_SPECS.get(instanceType);
        return com.optibrain.pricing.model.InstancePricing.builder()
                .instanceType(data.getInstanceType())
                .region(data.getRegion())
                .hourlyPrice(data.getHourlyPrice())
                .monthlyPrice(data.getMonthlyPrice())
                .currency("USD")
                .vcpu(spec == null ? null : String.valueOf(spec.vcpu()))
                .memory(spec == null ? null : spec.memoryGb() + " GB")
                .build();
    }

    /**
     * Find the cheapest instance that satisfies at least the requested vCPU and memory in a region.
     *
     * @return the matching instance pricing, or {@code null} when no instance meets the
     *         requirements in that region. Never returns an undersized instance with requested
     *         values painted over it.
     */
    public com.optibrain.pricing.model.InstancePricing cheapestWithAtLeast(int vcpus, int memoryGb, String region) {
        com.optibrain.pricing.model.InstancePricing best = null;
        for (Map.Entry<String, InstanceSpec> entry : INSTANCE_SPECS.entrySet()) {
            String type = entry.getKey();
            InstanceSpec spec = entry.getValue();
            if (spec.vcpu() >= vcpus && spec.memoryGb() >= memoryGb) {
                PricingData data = getPricingData(type, region);
                if (data == null) continue;
                com.optibrain.pricing.model.InstancePricing candidate =
                        com.optibrain.pricing.model.InstancePricing.builder()
                                .instanceType(type)
                                .region(region)
                                .hourlyPrice(data.getHourlyPrice())
                                .monthlyPrice(data.getMonthlyPrice())
                                .currency("USD")
                                .vcpu(String.valueOf(spec.vcpu()))
                                .memory(spec.memoryGb() + " GB")
                                .build();
                if (best == null || candidate.getHourlyPrice() < best.getHourlyPrice()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    private int getVcpuForType(String instanceType) {
        InstanceSpec spec = INSTANCE_SPECS.get(instanceType);
        return spec == null ? 0 : spec.vcpu();
    }

    private int getMemoryForType(String instanceType) {
        InstanceSpec spec = INSTANCE_SPECS.get(instanceType);
        return spec == null ? 0 : spec.memoryGb();
    }

    /**
     * Optional wrapper so legacy single-arg callers can migrate to {@link #get(String, String)}.
     * <p>Uses {@code us-east-1} as a fallback region only when no explicit region is available;
     * callers should prefer the two-argument form and pass the resource's actual region.
     */
    @Deprecated
    public com.optibrain.pricing.model.InstancePricing get(String instanceType) {
        return get(instanceType, "us-east-1");
    }
}
