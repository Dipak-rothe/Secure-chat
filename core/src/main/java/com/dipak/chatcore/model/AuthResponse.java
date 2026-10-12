package com.dipak.chatcore.model;

/** Server response for auth endpoints. */
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UserModel user;

    public AuthResponse() {}

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UserModel getUser() { return user; }
    public void setUser(UserModel user) { this.user = user; }
}
