package com.dipak.server.dto;

import com.dipak.server.domain.User;

import java.time.Instant;

/** Public representation of a user. Never includes password_hash or TOTP secret. */
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private Instant lastSeen;
    private Instant createdAt;
    private boolean online;

    public UserDto() {}

    public UserDto(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.lastSeen = user.getLastSeen();
        this.createdAt = user.getCreatedAt();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
}
