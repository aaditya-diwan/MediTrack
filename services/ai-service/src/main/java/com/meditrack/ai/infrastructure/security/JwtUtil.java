package com.meditrack.ai.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT verification for the ai-service.
 *
 * <p>Like the other resource services, ai-service does not issue tokens and has
 * no user store. Tokens are minted by the patient-service using the same shared
 * {@code jwt.secret}; here we only verify the signature and expiry and read the
 * username/roles claims.
 */
@Component
public class JwtUtil {

    private static final String CLAIM_ROLES = "roles";

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Object rolesObj = getAllClaimsFromToken(token).get(CLAIM_ROLES);
        if (rolesObj instanceof List<?> rawList) {
            return rawList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public boolean isTokenValid(String token) {
        try {
            return getAllClaimsFromToken(token).getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
