package com.dipak.server.service;

import com.dipak.server.domain.RefreshToken;
import com.dipak.server.domain.User;
import com.dipak.server.exception.AppException;
import com.dipak.server.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Manages opaque refresh tokens with rotation and reuse detection.
 *
 * Rotation: every /refresh call issues a new token and revokes the old one.
 * Reuse detection: if a revoked token is presented, the entire family
 *   (all tokens for that user) is immediately revoked, signalling possible theft.
 */
@Service
public class TokenService {

    private final RefreshTokenRepository tokenRepo;
    private final long refreshTokenExpiryDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(RefreshTokenRepository tokenRepo,
                        @Value("${app.jwt.refresh-token-expiry-days}") long refreshTokenExpiryDays) {
        this.tokenRepo = tokenRepo;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    /** Create and persist a new refresh token for the user. Returns the raw (unhashed) token. */
    @Transactional
    public String createRefreshToken(User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(sha256Hex(rawToken));
        entity.setExpiresAt(Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS));
        tokenRepo.save(entity);

        return rawToken;
    }

    /**
     * Validate and rotate the refresh token.
     * Returns the user associated with the token so the caller can issue a new access token.
     * Throws AppException(401) on any failure.
     */
    @Transactional
    public User rotateRefreshToken(String rawToken) {
        String hash = sha256Hex(rawToken);
        RefreshToken token = tokenRepo.findByTokenHash(hash)
                .orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));

        if (token.isRevoked()) {
            // Reuse detected — revoke entire family to limit damage
            tokenRepo.revokeAllByUserId(token.getUser().getId(), Instant.now());
            throw AppException.unauthorized("Refresh token reuse detected. Please log in again.");
        }

        if (token.isExpired()) {
            throw AppException.unauthorized("Refresh token expired");
        }

        // Revoke the used token (rotation)
        token.setRevokedAt(Instant.now());
        tokenRepo.save(token);

        return token.getUser();
    }

    /** Revoke all refresh tokens for a user (logout). */
    @Transactional
    public void revokeAll(Long userId) {
        tokenRepo.revokeAllByUserId(userId, Instant.now());
    }

    /** Nightly cleanup of expired tokens. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = tokenRepo.deleteExpiredBefore(Instant.now().minus(1, ChronoUnit.DAYS));
        if (deleted > 0) {
            // Only log the count, not any token values
            org.slf4j.LoggerFactory.getLogger(TokenService.class)
                    .info("Cleaned up {} expired refresh tokens", deleted);
        }
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
