package com.optibrain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.demo-mode:false}")
    private boolean demoMode;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> {
                // Demo mode only allowed in dev profile for security
                if (demoMode && "dev".equals(activeProfile)) {
                    auth.requestMatchers("/api/**").permitAll();
                }
                auth.requestMatchers("/h2-console/**", "/actuator/**", "/error", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated();
            })
            .formLogin(withDefaults())
            .httpBasic(withDefaults())
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // WARNING: In-memory users are for development only
        // For production, implement proper authentication (JWT, OAuth2, database-backed users)
        
        String adminPassword = System.getenv("ADMIN_PASSWORD");
        String userPassword = System.getenv("USER_PASSWORD");
        
        // Fallback to default passwords only in dev mode
        if ("dev".equals(activeProfile)) {
            adminPassword = adminPassword != null ? adminPassword : "admin123";
            userPassword = userPassword != null ? userPassword : "user123";
        } else {
            // Production mode requires environment variables
            if (adminPassword == null || userPassword == null) {
                throw new IllegalStateException(
                    "ADMIN_PASSWORD and USER_PASSWORD environment variables must be set in production mode"
                );
            }
        }
        
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode(adminPassword))
            .roles("ADMIN")
            .build();
        
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder.encode(userPassword))
            .roles("USER")
            .build();

        return new InMemoryUserDetailsManager(admin, user);
    }
}
