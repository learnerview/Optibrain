package com.optibrain.websocket.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebSocket chat transport.
 *
 * <p>Broadcasting is handled by {@code @SendTo}, so no {@code SimpMessagingTemplate} is
 * injected - the template was dead weight.
 *
 * <p>This relays messages without interpreting them. Answer generation belongs to the
 * ML service behind {@code PyBridgeService}; an echo here would be indistinguishable from
 * a real answer to a client.
 */
@Controller
@Slf4j
public class ChatWebSocketController {

    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public Map<String, Object> sendMessage(Map<String, String> message) {
        log.debug("Received WebSocket message: {}", message.get("message"));
        return Map.of(
            "type", "message",
            "content", message.get("message"),
            "timestamp", LocalDateTime.now().toString()
        );
    }

    @MessageMapping("/chat.join")
    @SendTo("/topic/chat")
    public Map<String, Object> joinChat(Map<String, String> user) {
        log.debug("User joined chat: {}", user.get("userId"));
        return Map.of(
            "type", "system",
            "content", "User " + user.get("userId") + " joined the chat",
            "timestamp", LocalDateTime.now().toString()
        );
    }
}