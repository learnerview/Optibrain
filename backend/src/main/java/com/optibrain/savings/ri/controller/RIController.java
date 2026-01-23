package com.optibrain.savings.ri.controller;

import com.optibrain.savings.ri.model.ReservedInstance;
import com.optibrain.savings.ri.model.SavingsPlan;
import com.optibrain.savings.ri.service.RIService;
import com.optibrain.savings.ri.service.SavingsPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ri")
@RequiredArgsConstructor
public class RIController {

    private final RIService riService;
    private final SavingsPlanService spService;

    @GetMapping("/coverage/{tenantId}")
    public ResponseEntity<Double> getRICoverage(@PathVariable String tenantId) {
        return ResponseEntity.ok(riService.calculateCoverage(tenantId));
    }

    @PostMapping("/recommend/{tenantId}")
    public ResponseEntity<List<ReservedInstance>> getRIRecommendations(@PathVariable String tenantId) {
        return ResponseEntity.ok(riService.getRecommendations(tenantId));
    }

    @GetMapping("/savings-plans/recommend/{tenantId}")
    public ResponseEntity<List<SavingsPlan>> getSPRecommendations(@PathVariable String tenantId) {
        return ResponseEntity.ok(spService.getRecommendations(tenantId));
    }
}
