package com.dipak.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET =
            "test-secret-key-that-is-at-least-256-bits-long-for-tests";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 15L);
    }

    @Test
    void generateAndValidateToken() {
        String token = jwtService.generateAccessToken(42L, "alice@example.com");
        assertNotNull(token);
        Claims claims = jwtService.validateAndGetClaims(token);
        assertEquals("42", claims.getSubject());
        assertEquals("alice@example.com", claims.get("email"));
    }

    @Test
    void getUserIdFromToken() {
        String token = jwtService.generateAccessToken(99L, "bob@test.com");
        assertEquals(99L, jwtService.getUserIdFromToken(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(1L, "test@test.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThrows(JwtException.class, () -> jwtService.validateAndGetClaims(tampered));
    }

    @Test
    void wrongSecretIsRejected() {
        String token = jwtService.generateAccessToken(1L, "test@test.com");
        JwtService otherService = new JwtService(
                "different-secret-key-that-is-at-least-256-bits-long", 15L);
        assertThrows(JwtException.class, () -> otherService.validateAndGetClaims(token));
    }

    @Test
    void tokenForDifferentUserHasDifferentSubject() {
        String token1 = jwtService.generateAccessToken(1L, "a@test.com");
        String token2 = jwtService.generateAccessToken(2L, "b@test.com");
        assertNotEquals(
                jwtService.getUserIdFromToken(token1),
                jwtService.getUserIdFromToken(token2)
        );
    }
}
