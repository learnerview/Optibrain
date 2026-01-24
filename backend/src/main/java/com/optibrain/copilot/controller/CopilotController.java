package com.optibrain.copilot.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.copilot.service.CopilotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/copilot")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class CopilotController {
    
    private final CopilotService copilotService;
    
    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            log.info("Copilot query: {}", message);
            
            Map<String, Object> response = copilotService.generateResponse(message);
            return ApiResponse.success(response, "Response generated");
        } catch (Exception e) {
            log.error("Error generating copilot response: {}", e.getMessage());
            return ApiResponse.error("Failed to generate response: " + e.getMessage());
        }
    }
}
