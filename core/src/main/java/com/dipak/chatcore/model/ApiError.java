package com.dipak.chatcore.model;

import java.util.List;

/** Matches the server error envelope: {status, message, errors, timestamp} */
public class ApiError {

    private int status;
    private String message;
    private List<String> errors;
    private String timestamp;

    public ApiError() {}

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
