package com.dipak.chatcore.api;

/**
 * Thrown by ApiClient when the server returns an error status (4xx, 5xx)
 * or the request fails due to a network problem.
 */
public class ApiException extends Exception {

    private static final long serialVersionUID = 1L;
    private final int httpStatus;
    private final String serverMessage;

    public ApiException(int httpStatus, String serverMessage) {
        super("HTTP " + httpStatus + ": " + serverMessage);
        this.httpStatus = httpStatus;
        this.serverMessage = serverMessage;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
        this.serverMessage = message;
    }

    /** HTTP status code, or -1 for network errors. */
    public int getHttpStatus() { return httpStatus; }

    /** The error message from the server response body, or the exception message. */
    public String getServerMessage() { return serverMessage; }

    public boolean isNetworkError() { return httpStatus == -1; }
    public boolean isUnauthorized() { return httpStatus == 401; }
    public boolean isTooManyRequests() { return httpStatus == 429; }
}
