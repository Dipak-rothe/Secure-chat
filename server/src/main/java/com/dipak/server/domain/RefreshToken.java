package com.dipak.server.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Rotating refresh tokens. Only the SHA-256 hash is stored.
 * If a revoked token is presented (reuse detection), the entire family is revoked
 * to protect against token theft.
 */
@Entity
@Table(name = "refresh_tokens",
       indexes = {
           @Index(name = "idx_rt_user_id", columnList = "user_id"),
           @Index(name = "idx_rt_token_hash", columnList = "token_hash", unique = true)
       })
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256(raw token), hex. The raw token is never stored. */
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Set when this token was replaced by a new one during rotation. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** ID of the token that replaced this one (for the revocation family). */
    @Column(name = "replaced_by")
    private Long replacedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    // --- Getters and setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public Long getReplacedBy() { return replacedBy; }
    public void setReplacedBy(Long replacedBy) { this.replacedBy = replacedBy; }

    public Instant getCreatedAt() { return createdAt; }
}
