package com.dipak.chatcore.api;

import com.dipak.chatcore.model.AuthResponse;
import com.dipak.chatcore.model.UserModel;
import com.dipak.chatcore.store.SecureKeyStorage;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientTest {

    private MockWebServer server;
    private InMemoryKeyStorage keyStorage;
    private ApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        keyStorage = new InMemoryKeyStorage();
        client = new ApiClient(server.url("/").toString(), keyStorage);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void publicPostDoesNotRequireToken() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"accessToken\":\"token123\",\"refreshToken\":\"ref123\",\"user\":{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@test.com\"}}"));

        Map<String, String> body = new HashMap<>();
        body.put("email", "alice@test.com");
        body.put("password", "pass");

        AuthResponse resp = client.postPublic("/api/auth/login", body, AuthResponse.class);
        assertNotNull(resp);
        assertEquals("token123", resp.getAccessToken());

        RecordedRequest req = server.takeRequest();
        assertNull(req.getHeader("Authorization"));
    }

    @Test
    void authenticatedGetSendsBearerToken() throws Exception {
        keyStorage.put("access_token", "my-jwt-token");
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@test.com\"}"));

        UserModel user = client.get("/api/users/me", UserModel.class);
        assertNotNull(user);
        assertEquals("Alice", user.getName());

        RecordedRequest req = server.takeRequest();
        assertEquals("Bearer my-jwt-token", req.getHeader("Authorization"));
    }

    @Test
    void automatic401RefreshAndRetry() throws Exception {
        keyStorage.put("access_token", "expired-token");
        keyStorage.put("refresh_token", "valid-refresh-token");

        // 1. First request returns 401
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("{\"status\":401,\"message\":\"Token expired\"}"));

        // 2. Token refresh call succeeds
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"accessToken\":\"new-token-456\",\"refreshToken\":\"new-refresh-789\"}"));

        // 3. Retry of original request succeeds with new token
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@test.com\"}"));

        UserModel user = client.get("/api/users/me", UserModel.class);
        assertNotNull(user);
        assertEquals("Alice", user.getName());
        assertEquals("new-token-456", keyStorage.get("access_token"));
        assertEquals("new-refresh-789", keyStorage.get("refresh_token"));

        assertEquals(3, server.getRequestCount());
    }

    @Test
    void errorResponseThrowsApiException() {
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"status\":404,\"message\":\"User not found\"}"));

        ApiException ex = assertThrows(ApiException.class, () ->
                client.get("/api/users/999", UserModel.class));
        assertEquals(404, ex.getHttpStatus());
        assertEquals("User not found", ex.getServerMessage());
    }

    private static class InMemoryKeyStorage implements SecureKeyStorage {
        private final Map<String, String> map = new HashMap<>();

        @Override
        public void put(String key, String value) { map.put(key, value); }

        @Override
        public String get(String key) { return map.get(key); }

        @Override
        public void remove(String key) { map.remove(key); }

        @Override
        public void clear() { map.clear(); }
    }
}
