package com.optibrain.auth.controller;

import com.optibrain.common.dto.ApiResponse;
import com.optibrain.auth.dto.LoginRequest;
import com.optibrain.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import com.optibrain.auth.security.JwtService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final com.optibrain.auth.security.UserStore userStore;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * Verifies credentials against the configured user store.
     *
     * <p>This previously returned {@code authenticated: true} for any input whatsoever,
     * including an empty username and password, and reported success unconditionally. An
     * endpoint whose name says "login" and whose result says "authenticated" must not
     * accept anonymous callers; the frontend cannot distinguish a real session from a
     * fabricated one.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
        @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String username = request.getUsername();
        if (username == null || username.isBlank() || request.getPassword() == null
                || request.getPassword().isBlank()) {
            return ResponseEntity.status(400)
                .body(ApiResponse.error("Username and password are required"));
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));

            // Issue a short-lived stateless JWT; the security filter validates it later.
            SecurityContextHolder.getContext().setAuthentication(auth);
            String token = jwtService.generate(auth.getName());

            Map<String, Object> userData = Map.of(
                    "username", auth.getName(),
                    "authenticated", true,
                    "authorities", auth.getAuthorities().stream()
                            .map(a -> a.getAuthority()).toList(),
                    "token", token
            );
            log.info("Successful login for {}", auth.getName());
            return ResponseEntity.ok(ApiResponse.success(userData, "Login successful"));
        } catch (AuthenticationException e) {
            log.warn("Rejected login for {}: {}", username, e.getClass().getSimpleName());
            return ResponseEntity.status(401)
                .body(ApiResponse.error("Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (userStore.exists(username)) {
            return ResponseEntity.status(409).body(ApiResponse.error("User already exists"));
        }

        org.springframework.security.core.userdetails.UserDetails user = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles("USER")
                .build();
        userStore.add(user, request.getCompany());
        String token = jwtService.generate(username);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "username", username,
                "authenticated", true,
                "authorities", user.getAuthorities().stream().map(a -> a.getAuthority()).toList(),
                "token", token
        ), "Registration successful"));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                jwtService.revoke(header.substring(7));
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
