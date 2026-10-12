package com.dipak.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Stateless JWT creation and validation.
 * The secret MUST be at least 256 bits (32 chars). In production it comes from
 * the JWT_SECRET environment variable via app.jwt.secret.
 *
 * Access tokens expire in 15 minutes. Refresh tokens are opaque random strings
 * handled by TokenService — NOT JWTs, to support revocation.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-minutes}") long accessTokenExpiryMinutes) {
        // HMAC-SHA256 key derived from the configured secret
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
    }

    /** Create a signed access JWT for the given user. */
    public String generateAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiryMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validate a token and return its claims.
     * Throws {@link JwtException} (a RuntimeException) on any failure;
     * callers translate this to 401.
     */
    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extract the user ID from a valid token. */
    public Long getUserIdFromToken(String token) {
        Claims claims = validateAndGetClaims(token);
        return Long.parseLong(claims.getSubject());
    }
}
