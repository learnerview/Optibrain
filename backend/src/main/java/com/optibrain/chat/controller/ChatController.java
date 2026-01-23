package com.optibrain.chat.controller;

import com.optibrain.chat.service.PyBridgeService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@lombok.RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
public class ChatController {

    private final PyBridgeService aiService;

    @PostMapping
    public java.util.Map<String, String> chat(@RequestBody ChatRequest request) {
        String aiResponse = aiService.generateResponse(request.getMessage());
        return java.util.Map.of("response", aiResponse);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        private String message;
    }
}
