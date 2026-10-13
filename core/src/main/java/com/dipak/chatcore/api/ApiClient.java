package com.dipak.chatcore.api;

import com.dipak.chatcore.model.ApiError;
import com.dipak.chatcore.model.AuthResponse;
import com.dipak.chatcore.store.SecureKeyStorage;
import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Core HTTP client wrapper.
 *
 * Features:
 * - Automatic token injection via Authorization header.
 * - Automatic 401 refresh-and-retry: on 401, fetches a new access token using
 *   the refresh token stored in SecureKeyStorage, then retries the original request once.
 * - Gson JSON serialisation/deserialisation.
 *
 * Thread safety: OkHttpClient is thread-safe. The refresh logic uses synchronized
 * to prevent thundering-herd (multiple concurrent 401s all trying to refresh at once).
 */
public class ApiClient {

    private static final String KEY_ACCESS_TOKEN  = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http;
    private final Gson gson;
    private final String baseUrl;
    private final SecureKeyStorage keyStorage;

    // Lock object for the refresh-and-retry critical section
    private final Object refreshLock = new Object();

    public ApiClient(String baseUrl, SecureKeyStorage keyStorage) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.keyStorage = keyStorage;
        this.gson = new Gson();
        this.http = new OkHttpClient.Builder()
                .protocols(java.util.Collections.singletonList(Protocol.HTTP_1_1))
                .retryOnConnectionFailure(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /** Constructor for tests that supply a custom OkHttpClient (e.g. MockWebServer). */
    public ApiClient(String baseUrl, SecureKeyStorage keyStorage, OkHttpClient http) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.keyStorage = keyStorage;
        this.gson = new Gson();
        this.http = http;
    }

    // -----------------------------------------------------------------------
    // Public helpers
    // -----------------------------------------------------------------------

    /** Execute a GET request and deserialise the JSON response. */
    public <T> T get(String path, Class<T> responseType) throws ApiException {
        Request request = buildRequest(path).get().build();
        return executeWithRetry(request, responseType);
    }

    /** Execute a POST request with a JSON body. */
    public <T> T post(String path, Object body, Class<T> responseType) throws ApiException {
        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);
        Request request = buildRequest(path).post(requestBody).build();
        return executeWithRetry(request, responseType);
    }

    /** Execute a PUT request with a JSON body. */
    public <T> T put(String path, Object body, Class<T> responseType) throws ApiException {
        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);
        Request request = buildRequest(path).put(requestBody).build();
        return executeWithRetry(request, responseType);
    }

    /** Execute a POST without authentication (for login / register / refresh). */
    public <T> T postPublic(String path, Object body, Class<T> responseType) throws ApiException {
        RequestBody requestBody = RequestBody.create(gson.toJson(body), JSON);
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .header("X-Pinggy-No-Screen", "true")
                .post(requestBody)
                .build();
        return execute(request, responseType);
    }

    /** Store tokens after a successful login/register/refresh call. */
    public void storeTokens(String accessToken, String refreshToken) {
        keyStorage.put(KEY_ACCESS_TOKEN, accessToken);
        keyStorage.put(KEY_REFRESH_TOKEN, refreshToken);
    }

    /** Clear all stored tokens on logout. */
    public void clearTokens() {
        keyStorage.remove(KEY_ACCESS_TOKEN);
        keyStorage.remove(KEY_REFRESH_TOKEN);
    }

    /** Returns true if an access token is stored (user appears logged in). */
    public boolean hasAccessToken() {
        String token = keyStorage.get(KEY_ACCESS_TOKEN);
        return token != null && !token.isEmpty();
    }

    public String getBaseUrl() { return baseUrl; }

    // -----------------------------------------------------------------------
    // Private implementation
    // -----------------------------------------------------------------------

    private Request.Builder buildRequest(String path) {
        String token = keyStorage.get(KEY_ACCESS_TOKEN);
        Request.Builder builder = new Request.Builder()
                .url(baseUrl + path)
                .header("X-Pinggy-No-Screen", "true");
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    /**
     * Execute the request. On 401, attempt one token refresh and retry.
     * The refresh is synchronised so only one thread refreshes at a time.
     */
    private <T> T executeWithRetry(Request request, Class<T> responseType) throws ApiException {
        try {
            return execute(request, responseType);
        } catch (ApiException ex) {
            if (!ex.isUnauthorized()) {
                throw ex;
            }
        }

        // 401: try to refresh the access token
        synchronized (refreshLock) {
            // Another thread may have already refreshed — check if the stored token changed
            String freshToken = keyStorage.get(KEY_ACCESS_TOKEN);
            String originalToken = request.header("Authorization");
            if (freshToken != null && !freshToken.isEmpty()
                    && ("Bearer " + freshToken).equals(originalToken)) {
                // Token did not change; we need to actually refresh
                performTokenRefresh();
            }
        }

        // Retry with the new token
        Request retried = buildRequest(request.url().encodedPath()).method(
                request.method(), request.body()).build();
        return execute(retried, responseType);
    }

    private void performTokenRefresh() throws ApiException {
        String refreshToken = keyStorage.get(KEY_REFRESH_TOKEN);
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ApiException(401, "No refresh token available");
        }
        String body = "{\"refreshToken\":\"" + refreshToken + "\"}";
        RequestBody requestBody = RequestBody.create(body, JSON);
        Request refreshRequest = new Request.Builder()
                .url(baseUrl + "/api/auth/refresh")
                .post(requestBody)
                .build();
        AuthResponse resp = execute(refreshRequest, AuthResponse.class);
        storeTokens(resp.getAccessToken(), resp.getRefreshToken());
    }

    private <T> T execute(Request request, Class<T> responseType) throws ApiException {
        try (Response response = http.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (response.isSuccessful()) {
                if (responseType == Void.class || body.isEmpty()) {
                    return null;
                }
                return gson.fromJson(body, responseType);
            } else {
                // Parse server error envelope
                String message = "HTTP " + response.code();
                try {
                    ApiError error = gson.fromJson(body, ApiError.class);
                    if (error != null && error.getMessage() != null) {
                        message = error.getMessage();
                    }
                } catch (Exception ignored) {
                    // Fall back to the status message
                }
                throw new ApiException(response.code(), message);
            }
        } catch (IOException ex) {
            throw new ApiException("Network error: " + ex.getMessage(), ex);
        }
    }
}
