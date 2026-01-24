package com.optibrain.websocket.controller;

import com.optibrain.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public Map<String, Object> sendMessage(Map<String, String> message) {
        log.info("Received WebSocket message: {}", message.get("message"));
        
        // In a real implementation, this would call the ML service
        // For now, return a simple response
        return Map.of(
            "type", "message",
            "content", "Received: " + message.get("message"),
            "timestamp", LocalDateTime.now().toString()
        );
    }
    
    @MessageMapping("/chat.join")
    @SendTo("/topic/chat")
    public Map<String, Object> joinChat(Map<String, String> user) {
        log.info("User joined chat: {}", user.get("userId"));
        
        return Map.of(
            "type", "system",
            "content", "User " + user.get("userId") + " joined the chat",
            "timestamp", LocalDateTime.now().toString()
        );
    }
}
