package com.dipak.server.dto;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error envelope returned by {@link com.dipak.server.exception.GlobalExceptionHandler}.
 * Format: {"status":400,"message":"...","errors":["..."],"timestamp":"..."}
 */
public class ErrorResponse {

    private final int status;
    private final String message;
    private final List<String> errors;
    private final Instant timestamp;

    public ErrorResponse(int status, String message, List<String> errors) {
        this.status = status;
        this.message = message;
        this.errors = errors;
        this.timestamp = Instant.now();
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public List<String> getErrors() { return errors; }
    public Instant getTimestamp() { return timestamp; }
}
