package com.dipak.server.dto;

import com.dipak.server.domain.UserKey;

import java.time.Instant;

/** DTO for uploading or fetching a user's public key bundle. */
public class UserKeyDto {

    private Long userId;
    private String publicKey;
    private String keyFingerprint;
    private String encryptedPrivateKeyBackup;
    private String kdfSalt;
    private Integer kdfIterations;
    private Instant createdAt;

    public UserKeyDto() {}

    public UserKeyDto(UserKey key) {
        this.userId = key.getUserId();
        this.publicKey = key.getPublicKey();
        this.keyFingerprint = key.getKeyFingerprint();
        this.encryptedPrivateKeyBackup = key.getEncryptedPrivateKeyBackup();
        this.kdfSalt = key.getKdfSalt();
        this.kdfIterations = key.getKdfIterations();
        this.createdAt = key.getCreatedAt();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getKeyFingerprint() { return keyFingerprint; }
    public void setKeyFingerprint(String keyFingerprint) { this.keyFingerprint = keyFingerprint; }

    public String getEncryptedPrivateKeyBackup() { return encryptedPrivateKeyBackup; }
    public void setEncryptedPrivateKeyBackup(String v) { this.encryptedPrivateKeyBackup = v; }

    public String getKdfSalt() { return kdfSalt; }
    public void setKdfSalt(String kdfSalt) { this.kdfSalt = kdfSalt; }

    public Integer getKdfIterations() { return kdfIterations; }
    public void setKdfIterations(Integer kdfIterations) { this.kdfIterations = kdfIterations; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
