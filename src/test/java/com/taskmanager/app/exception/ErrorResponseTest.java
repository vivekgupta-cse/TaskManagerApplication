package com.taskmanager.app.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ErrorResponse — the structured JSON error body.
 *
 * Tests both construction paths:
 * 1. The 3-arg constructor (used by most handlers)
 * 2. The Lombok @Builder path (used by validation and duplicate-task handlers)
 */
class ErrorResponseTest {

    // =========================================================================
    // 3-arg constructor
    // =========================================================================
    @Nested
    @DisplayName("3-arg constructor")
    class ThreeArgConstructor {

        @Test
        @DisplayName("sets status, error, message and auto-generates timestamp")
        void setsAllFieldsAndTimestamp() {
            LocalDateTime before = LocalDateTime.now();

            ErrorResponse response = new ErrorResponse(404, "Not Found", "Task missing");

            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(response.getError()).isEqualTo("Not Found");
            assertThat(response.getMessage()).isEqualTo("Task missing");
            assertThat(response.getTimestamp()).isNotNull();
            assertThat(response.getTimestamp()).isAfterOrEqualTo(before);
            // errors map should be null when not set
            assertThat(response.getErrors()).isNull();
        }
    }

    // =========================================================================
    // Lombok @Builder
    // =========================================================================
    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds response with all fields including errors map")
        void buildsWithAllFields() {
            LocalDateTime now = LocalDateTime.now();
            Map<String, String> fieldErrors = Map.of("title", "required", "completed", "missing");

            ErrorResponse response = ErrorResponse.builder()
                    .status(400)
                    .error("Bad Request")
                    .message("Validation failed")
                    .errors(fieldErrors)
                    .timestamp(now)
                    .build();

            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getError()).isEqualTo("Bad Request");
            assertThat(response.getMessage()).isEqualTo("Validation failed");
            assertThat(response.getErrors()).hasSize(2);
            assertThat(response.getErrors()).containsEntry("title", "required");
            assertThat(response.getErrors()).containsEntry("completed", "missing");
            assertThat(response.getTimestamp()).isEqualTo(now);
        }

        @Test
        @DisplayName("builds response without errors map (e.g. for DuplicateTaskException)")
        void buildsWithoutErrorsMap() {
            ErrorResponse response = ErrorResponse.builder()
                    .status(409)
                    .error("Conflict")
                    .message("Duplicate task")
                    .timestamp(LocalDateTime.now())
                    .build();

            assertThat(response.getStatus()).isEqualTo(409);
            assertThat(response.getErrors()).isNull();
        }
    }

    // =========================================================================
    // @AllArgsConstructor (all 5 fields)
    // =========================================================================
    @Nested
    @DisplayName("All-args constructor")
    class AllArgsConstructor {

        @Test
        @DisplayName("accepts all 5 parameters including errors map")
        void acceptsAllFiveParams() {
            LocalDateTime now = LocalDateTime.now();
            Map<String, String> errors = Map.of("field", "error msg");

            ErrorResponse response = new ErrorResponse(
                    500, "Internal Server Error", "Something went wrong", errors, now);

            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getError()).isEqualTo("Internal Server Error");
            assertThat(response.getMessage()).isEqualTo("Something went wrong");
            assertThat(response.getErrors()).containsEntry("field", "error msg");
            assertThat(response.getTimestamp()).isEqualTo(now);
        }
    }
}

