package com.taskmanager.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
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
        // 1. Create a map of all field errors
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        // 2. Build the structured response
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())           // 400
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())  // "Bad Request"
                .message("Validation failed for one or more fields")
                .errors(fieldErrors) // Plug in the map here
                .timestamp(LocalDateTime.now())
                .build();
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

    @ExceptionHandler(DuplicateTaskException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTask(DuplicateTaskException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value()) // 409 Conflict
                .error("Conflict")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                // We don't call .errors() here because there's no map for this error
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
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
