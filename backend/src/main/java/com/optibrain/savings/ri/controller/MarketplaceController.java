package com.optibrain.savings.ri.controller;

import com.optibrain.savings.ri.model.MarketplaceListing;
import com.optibrain.savings.ri.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ri/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/listings")
    public ResponseEntity<List<MarketplaceListing>> getListings(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String instanceType) {
        return ResponseEntity.ok(marketplaceService.getAvailableListings(region, instanceType));
    }
    
    @PostMapping("/sell/{tenantId}")
    public ResponseEntity<MarketplaceListing> sellRI(
            @PathVariable String tenantId,
            @RequestBody Map<String, Object> payload) {
        
        String riId = (String) payload.get("riId");
        double price = Double.parseDouble(String.valueOf(payload.get("price")));
        
        return ResponseEntity.ok(marketplaceService.sellRI(tenantId, riId, price));
    }
}
