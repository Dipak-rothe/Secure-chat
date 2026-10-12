package com.dipak.chatcore.api;

import com.dipak.chatcore.model.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * High-level API facade that maps to the server's REST endpoints.
 * All methods are blocking and should be called from a background thread.
 */
public class ChatApi {

    private final ApiClient client;

    public ChatApi(ApiClient client) {
        this.client = client;
    }

    // --- Auth ---

    public AuthResponse register(String name, String email, String password) throws ApiException {
        RegisterRequest req = new RegisterRequest(name, email, password);
        AuthResponse resp = client.postPublic("/api/auth/register", req, AuthResponse.class);
        client.storeTokens(resp.getAccessToken(), resp.getRefreshToken());
        return resp;
    }

    public AuthResponse login(String email, String password) throws ApiException {
        LoginRequest req = new LoginRequest(email, password);
        AuthResponse resp = client.postPublic("/api/auth/login", req, AuthResponse.class);
        client.storeTokens(resp.getAccessToken(), resp.getRefreshToken());
        return resp;
    }

    public void logout() throws ApiException {
        try {
            client.post("/api/auth/logout", new Object(), Void.class);
        } finally {
            client.clearTokens();
        }
    }

    // --- Users ---

    public UserModel getMe() throws ApiException {
        return client.get("/api/users/me", UserModel.class);
    }

    public List<UserModel> searchUsers(String query) throws ApiException {
        UserModel[] users = client.get("/api/users/search?q=" + urlEncode(query), UserModel[].class);
        if (users == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(users);
    }

    public ApiClient getClient() { return client; }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s; // UTF-8 is standard
        }
    }
}
