package com.dipak.server.exception;

/**
 * Domain exception that carries an HTTP status code.
 * Thrown by services when business rules are violated (e.g., duplicate email,
 * wrong password, resource not found).
 */
public class AppException extends RuntimeException {

    private final int status;

    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }

    // Convenience factories
    public static AppException notFound(String message) {
        return new AppException(404, message);
    }

    public static AppException badRequest(String message) {
        return new AppException(400, message);
    }

    public static AppException unauthorized(String message) {
        return new AppException(401, message);
    }

    public static AppException forbidden(String message) {
        return new AppException(403, message);
    }

    public static AppException conflict(String message) {
        return new AppException(409, message);
    }

    public static AppException tooManyRequests(String message) {
        return new AppException(429, message);
    }
}
