package com.dipak.server.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Stores the user's public EC key and the encrypted backup of their private key.
 * The server CANNOT decrypt encrypted_private_key_backup — it is AES-GCM encrypted
 * with a key derived from the user's password (PBKDF2). The server only holds
 * ciphertext so it can deliver it to a new device at login.
 */
@Entity
@Table(name = "user_keys")
public class UserKey {

    /** Same PK as users.id — one key record per user. */
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    /** X.509 SPKI DER bytes, Base64-encoded. */
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    /** SHA-256 of publicKey bytes displayed as hex groups for safety number comparison. */
    @Column(name = "key_fingerprint", nullable = false, length = 80)
    private String keyFingerprint;

    /** PKCS8 private key, AES-256-GCM encrypted with PBKDF2 key. Never decryptable by server. */
    @Column(name = "encrypted_private_key_backup", columnDefinition = "TEXT")
    private String encryptedPrivateKeyBackup;

    /** Base64-encoded 16-byte random salt used in PBKDF2. */
    @Column(name = "kdf_salt", length = 32)
    private String kdfSalt;

    @Column(name = "kdf_iterations")
    private Integer kdfIterations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }

    // --- Getters and setters ---

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getKeyFingerprint() { return keyFingerprint; }
    public void setKeyFingerprint(String keyFingerprint) { this.keyFingerprint = keyFingerprint; }

    public String getEncryptedPrivateKeyBackup() { return encryptedPrivateKeyBackup; }
    public void setEncryptedPrivateKeyBackup(String encryptedPrivateKeyBackup) {
        this.encryptedPrivateKeyBackup = encryptedPrivateKeyBackup;
    }

    public String getKdfSalt() { return kdfSalt; }
    public void setKdfSalt(String kdfSalt) { this.kdfSalt = kdfSalt; }

    public Integer getKdfIterations() { return kdfIterations; }
    public void setKdfIterations(Integer kdfIterations) { this.kdfIterations = kdfIterations; }

    public Instant getCreatedAt() { return createdAt; }
}
