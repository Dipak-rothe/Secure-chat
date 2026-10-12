package com.dipak.chatcore.model;

/** Minimal user model shared between desktop and Android. */
public class UserModel {

    private long id;
    private String name;
    private String email;
    private String lastSeen;   // ISO-8601 string
    private boolean online;

    public UserModel() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLastSeen() { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    @Override
    public String toString() {
        return "UserModel{id=" + id + ", name='" + name + "', email='" + email + "'}";
    }
}
