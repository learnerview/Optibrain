package com.optibrain.savings.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.savings.model.SavingsReport;
import com.optibrain.savings.service.SavingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/savings")
@RequiredArgsConstructor
@org.springframework.web.bind.annotation.CrossOrigin("*")
public class SavingsController {

    private final SavingsService savingsService;

    @GetMapping("/summary")
    public ApiResponse<SavingsReport> getSummary() {
        return ApiResponse.success(savingsService.generateReport(), "Savings summary retrieved");
    }
}
