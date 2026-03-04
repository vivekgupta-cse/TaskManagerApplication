package com.taskmanager.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for SanitizationService.
 * <p>
 * SanitizationService has no Spring dependencies, so we instantiate it directly.
 * No mocking needed — we test the real AntiSamy sanitization logic.
 */
class SanitizationServiceTest {

    private SanitizationService sanitizationService;

    @BeforeEach
    void setUp() {
        sanitizationService = new SanitizationService();
    }

    @Nested
    @DisplayName("sanitize() — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("returns null when input is null")
        void returnsNullForNullInput() {
            assertThat(sanitizationService.sanitize(null)).isNull();
        }

        @Test
        @DisplayName("returns blank string when input is blank")
        void returnsBlankForBlankInput() {
            assertThat(sanitizationService.sanitize("   ")).isEqualTo("   ");
        }

        @Test
        @DisplayName("returns empty string when input is empty")
        void returnsEmptyForEmptyInput() {
            assertThat(sanitizationService.sanitize("")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("sanitize() — safe inputs")
    class SafeInputs {

        @Test
        @DisplayName("leaves plain text unchanged")
        void leavesPlainTextUnchanged() {
            String input = "Buy groceries";
            assertThat(sanitizationService.sanitize(input)).isEqualTo("Buy groceries");
        }

        @Test
        @DisplayName("leaves text with numbers unchanged")
        void leavesTextWithNumbersUnchanged() {
            assertThat(sanitizationService.sanitize("Task 123")).isEqualTo("Task 123");
        }
    }

    @Nested
    @DisplayName("sanitize() — dangerous inputs")
    class DangerousInputs {

        @Test
        @DisplayName("strips script tags from input")
        void stripsScriptTags() {
            String dirty = "<script>alert('xss')</script>Buy groceries";
            String clean = sanitizationService.sanitize(dirty);
            assertThat(clean).doesNotContain("<script>");
            assertThat(clean).doesNotContain("alert");
        }

        @Test
        @DisplayName("strips javascript: URL from input")
        void stripsJavascriptUrl() {
            String dirty = "<a href=\"javascript:alert(1)\">click me</a>";
            String clean = sanitizationService.sanitize(dirty);
            assertThat(clean).doesNotContain("javascript:");
        }

        @Test
        @DisplayName("strips onclick event handler")
        void stripsOnClickHandler() {
            String dirty = "<div onclick=\"alert('xss')\">content</div>";
            String clean = sanitizationService.sanitize(dirty);
            assertThat(clean).doesNotContain("onclick");
            assertThat(clean).doesNotContain("alert");
        }

        @Test
        @DisplayName("strips img onerror injection")
        void stripsImgOnError() {
            String dirty = "<img src=x onerror=alert(1)>";
            String clean = sanitizationService.sanitize(dirty);
            assertThat(clean).doesNotContain("onerror");
        }
    }

    @Nested
    @DisplayName("sanitize() — policy/engine failures")
    class FailureModes {

        @Test
        @DisplayName("returns input when policy is unavailable")
        void returnsInputWhenPolicyUnavailable() {
            ReflectionTestUtils.setField(sanitizationService, "policy", null);
            String result = sanitizationService.sanitize("Hello");
            assertThat(result).isEqualTo("Hello");
        }

        @Test
        @DisplayName("returns input when AntiSamy throws")
        void returnsInputWhenAntiSamyThrows() {
            Policy mockPolicy = mock(Policy.class);
            ReflectionTestUtils.setField(sanitizationService, "policy", mockPolicy);
            AntiSamy throwingAntiSamy = new AntiSamy() {
                @Override
                public org.owasp.validator.html.CleanResults scan(String input, Policy pol) {
                    throw new RuntimeException("boom");
                }
            };
            ReflectionTestUtils.setField(sanitizationService, "antiSamy", throwingAntiSamy);
            String result = sanitizationService.sanitize("Hello");
            assertThat(result).isEqualTo("Hello");
        }
    }
}
