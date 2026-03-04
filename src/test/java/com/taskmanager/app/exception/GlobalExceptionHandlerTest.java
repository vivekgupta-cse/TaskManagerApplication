package com.taskmanager.app.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests for GlobalExceptionHandler.
 *
 * We call the handler methods directly — no MockMvc, no Spring context.
 * This lets us verify the response status codes and ErrorResponse bodies
 * without any HTTP overhead.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleTaskNotFound returns 404 with correct error body")
    void handleTaskNotFound_Returns404() {
        TaskNotFoundException ex = new TaskNotFoundException(42L);

        ResponseEntity<ErrorResponse> response = handler.handleTaskNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Task with ID 42 not found");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleDuplicateTask returns 409 Conflict with message")
    void handleDuplicateTask_Returns409() {
        DuplicateTaskException ex = new DuplicateTaskException("You already have an active task with this title!");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateTask(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).contains("active task");
    }

    @Test
    @DisplayName("handleBadRequest returns 400 with exception message")
    void handleBadRequest_Returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input provided");

        ResponseEntity<ErrorResponse> response = handler.handleBadRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input provided");
    }

    @Test
    @DisplayName("handleValidationErrors returns 400 with field errors map")
    void handleValidationErrors_Returns400WithFieldErrorsMap() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError titleError = new FieldError("taskRequestDTO", "title", "Title is required");
        FieldError completedError = new FieldError("taskRequestDTO", "completed", "Completion status must be specified");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(titleError, completedError));

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed for one or more fields");
        assertThat(response.getBody().getErrors()).containsEntry("title", "Title is required");
        assertThat(response.getBody().getErrors()).containsEntry("completed", "Completion status must be specified");
    }

    @Test
    @DisplayName("handleGeneric returns 500 with safe message — no internal detail exposed")
    void handleGeneric_Returns500WithSafeMessage() {
        Exception ex = new RuntimeException("DB connection refused at 192.168.1.10:5432");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage())
                .isEqualTo("Something went wrong. Please try again later.")
                .doesNotContain("DB connection")
                .doesNotContain("192.168");
    }

    @Test
    @DisplayName("ErrorResponse always has a non-null timestamp")
    void errorResponse_AlwaysHasTimestamp() {
        TaskNotFoundException ex = new TaskNotFoundException(1L);
        ResponseEntity<ErrorResponse> response = handler.handleTaskNotFound(ex);

        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}

