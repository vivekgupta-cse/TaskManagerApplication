package com.taskmanager.app.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DuplicateTaskException.
 *
 * Verifies the exception message is preserved.
 */
class DuplicateTaskExceptionTest {

    @Test
    @DisplayName("message is preserved from constructor")
    void messageIsPreserved() {
        String msg = "You already have an active task with this title!";
        DuplicateTaskException ex = new DuplicateTaskException(msg);

        assertThat(ex.getMessage()).isEqualTo(msg);
    }

    @Test
    @DisplayName("extends RuntimeException")
    void isRuntimeException() {
        DuplicateTaskException ex = new DuplicateTaskException("test");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}

