package com.optibrain.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtService {

    static final String DEV_DEFAULT_SECRET = "OPTIBRAIN_DEV_JWT_SECRET_MEAN_CHANGE_IN_PROD_000";

    private static final int MAX_REVOKED = 1000;

    private final SecretKey key;
    private final long expirationMillis;
    private final Set<String> revoked = new HashSet<>();
    private final Deque<String> revokeOrder = new ArrayDeque<>();

    public JwtService(
            @Value("${app.jwt.secret:" + DEV_DEFAULT_SECRET + "}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMillis,
            @Value("${spring.profiles.active:}") String activeProfiles) {
        if (activeProfiles != null && List.of(activeProfiles.split(",")).contains("prod")
                && DEV_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException("app.jwt.secret must be set to a real secret when"
                    + " the prod profile is active; refusing to sign sessions with the dev default");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String generate(String subject) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    public String validateAndGetSubject(String token) {
        if (isRevoked(token)) {
            throw new io.jsonwebtoken.JwtException("Token has been revoked");
        }
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Permanently invalidates a token, bounded so a flood of logouts cannot grow memory
     * without limit. Revocation lives in memory: a restarted node forgets it, which is
     * why production must keep access tokens short-lived.
     */
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (revoked.add(token)) {
            revokeOrder.addLast(token);
            while (revokeOrder.size() > MAX_REVOKED) {
                revoked.remove(revokeOrder.removeFirst());
            }
        }
    }

    boolean isRevoked(String token) {
        return revoked.contains(token);
    }
}