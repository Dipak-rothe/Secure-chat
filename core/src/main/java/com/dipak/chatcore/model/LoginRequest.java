package com.dipak.chatcore.model;

/** Request payload for POST /api/auth/login */
public class LoginRequest {

    private String email;
    private String password;
    private String totpCode;

    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public LoginRequest(String email, String password, String totpCode) {
        this.email = email;
        this.password = password;
        this.totpCode = totpCode;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTotpCode() { return totpCode; }
    public void setTotpCode(String totpCode) { this.totpCode = totpCode; }
}
