package com.taskmanager.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/*
@RestControllerAdvice
Think of this as a Global Interceptor.

@ControllerAdvice: Tells Spring, "This class contains logic that applies to all controllers in the entire project."

@ResponseBody: (Combined into RestControllerAdvice) ensures that whatever the method returns is automatically
converted to JSON.
 */

@RestControllerAdvice // This catches exceptions across all controllers
public class GlobalExceptionHandler {

    // Handles TaskNotFoundException specifically → 404 Not Found
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),       // 404
                HttpStatus.NOT_FOUND.getReasonPhrase(), // "Not Found"
                ex.getMessage()                     // "Task with ID 999 not found"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Handles @Valid failures on @RequestBody (e.g. @NotBlank, @Size violations) → 400 Bad Request
    // Triggered when client sends invalid input to POST /api/tasks or PUT /api/tasks/{id}
    // Example: missing title → "title: Title is required"
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Collect all field-level validation messages into one string
        // e.g. "title: Title is required; description: Description cannot exceed 500 characters"
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),         // 400
                HttpStatus.BAD_REQUEST.getReasonPhrase(), // "Bad Request"
                message
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Handles bad input (e.g., invalid arguments) → 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),         // 400
                HttpStatus.BAD_REQUEST.getReasonPhrase(), // "Bad Request"
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Catch-all for any other unexpected exception → 500 Internal Server Error
    // Never expose internal details (ex.getMessage()) to the client!
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),         // 500
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), // "Internal Server Error"
                "Something went wrong. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
