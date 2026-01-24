package com.optibrain.auth.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.auth.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AuthController {
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
        @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        try {
            log.info("Login attempt for user: {}", request.getEmail());
            
            // In demo mode, accept any credentials
            // In production, Spring Security would validate credentials
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            Map<String, Object> userData = Map.of(
                "email", request.getEmail(),
                "authenticated", true,
                "message", "Login successful (demo mode)"
            );
            
            return ResponseEntity.ok(ApiResponse.success(userData, "Login successful"));
        } catch (Exception e) {
            log.error("Login failed for user {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(401)
                .body(ApiResponse.error("Invalid credentials"));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        try {
            log.info("Logout request");
            if (request.getSession(false) != null) {
                request.getSession().invalidate();
            }
            return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(null, "Logout completed"));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        boolean isAuthenticated = auth != null && auth.isAuthenticated() 
            && !auth.getName().equals("anonymousUser");
        
        Map<String, Object> status = Map.of(
            "authenticated", isAuthenticated,
            "user", isAuthenticated ? auth.getName() : "anonymous"
        );
        
        return ResponseEntity.ok(ApiResponse.success(status, "Auth status retrieved"));
    }
}
