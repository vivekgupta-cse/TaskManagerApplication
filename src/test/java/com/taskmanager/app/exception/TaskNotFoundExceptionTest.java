package com.taskmanager.app.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TaskNotFoundException.
 *
 * Verifies the exception message format includes the missing task ID.
 */
class TaskNotFoundExceptionTest {

    @Test
    @DisplayName("message contains the task ID")
    void messageContainsId() {
        TaskNotFoundException ex = new TaskNotFoundException(42L);

        assertThat(ex.getMessage()).isEqualTo("Task with ID 42 not found");
    }

    @Test
    @DisplayName("extends RuntimeException")
    void isRuntimeException() {
        TaskNotFoundException ex = new TaskNotFoundException(1L);

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}

