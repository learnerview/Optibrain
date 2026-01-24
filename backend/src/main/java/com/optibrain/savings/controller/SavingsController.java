package com.optibrain.savings.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.savings.model.SavingsReport;
import com.optibrain.savings.service.SavingsProjectionService;
import com.optibrain.savings.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@org.springframework.web.bind.annotation.CrossOrigin("*")
public class SavingsController {

    private final SavingsProjectionService savingsService;

    @GetMapping("/savings")
    public ApiResponse<Map<String, Object>> getSummary() {
        return ApiResponse.success(savingsService.getSavingsProjection(), "Savings summary retrieved");
    }
}
