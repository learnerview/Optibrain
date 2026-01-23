package com.optibrain.savings.ri.service;

import com.optibrain.savings.ri.model.MarketplaceListing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class MarketplaceService {
    
    public List<MarketplaceListing> getAvailableListings(String region, String instanceType) {
        // Mock data generator
        List<MarketplaceListing> listings = new ArrayList<>();
        Random rand = new Random();
        
        int count = rand.nextInt(5) + 1;
        for (int i = 0; i < count; i++) {
            double originalPrice = 500 + rand.nextDouble() * 1000;
            double discount = 0.1 + rand.nextDouble() * 0.3; // 10-40% discount
            
            listings.add(MarketplaceListing.builder()
                    .id(UUID.randomUUID().toString())
                    .riId("ri-" + UUID.randomUUID().toString().substring(0, 8))
                    .sellerTenantId("external-tenant-" + rand.nextInt(100))
                    .instanceType(instanceType != null ? instanceType : "m5.large")
                    .region(region != null ? region : "us-east-1")
                    .remainingTermMonths(rand.nextInt(10) + 2)
                    .price(originalPrice * (1 - discount))
                    .originalPrice(originalPrice)
                    .savingsPercentage(discount * 100)
                    .status("ACTIVE")
                    .postedAt(Instant.now().minusSeconds(rand.nextInt(86400 * 7)))
                    .build());
        }
        
        log.info("Found {} marketplace listings for {} in {}", listings.size(), instanceType, region);
        return listings;
    }
    
    public MarketplaceListing sellRI(String tenantId, String riId, double price) {
        log.info("Listing RI {} for sale by tenant {} at ${}", riId, tenantId, price);
        return MarketplaceListing.builder()
                .id(UUID.randomUUID().toString())
                .riId(riId)
                .sellerTenantId(tenantId)
                .instanceType("m5.large") // Mock lookup
                .region("us-east-1") // Mock lookup
                .remainingTermMonths(6) // Mock lookup
                .price(price)
                .originalPrice(price * 1.2)
                .savingsPercentage(16.6)
                .status("ACTIVE")
                .postedAt(Instant.now())
                .build();
    }
}
