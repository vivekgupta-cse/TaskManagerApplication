package com.example.TaskManagerApplication.exception;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * A structured error body returned by GlobalExceptionHandler
 * for every error response, instead of a plain String.
 *
 * Example JSON output:
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Task with ID 999 not found",
 *   "timestamp": "2026-02-22T10:30:00"
 * }
 */
@Getter // Lombok generates all getters — Jackson needs them to serialize this object to JSON
public class ErrorResponse {

    private final int status;            // HTTP status code  e.g. 404
    private final String error;          // HTTP status name  e.g. "Not Found"
    private final String message;        // Human-readable description of the problem
    private final LocalDateTime timestamp; // When the error occurred

    // Constructor used by GlobalExceptionHandler
    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = LocalDateTime.now(); // Automatically set to current time
    }
}

