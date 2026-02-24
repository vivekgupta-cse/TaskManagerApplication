package com.taskmanager.app.service;

import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * SanitizationService — strips dangerous HTML/JavaScript from user-supplied text.
 *
 * Why is this needed?
 * -------------------
 * When a user sends text like:
 *   "title": "<script>alert('Hacked!')</script>"
 *
 * Without sanitization, that malicious script gets stored in the database.
 * If any frontend ever renders it, the script runs in the browser — this is
 * called XSS (Cross-Site Scripting).
 *
 * AntiSamy (by OWASP) parses the input against a known-safe policy and removes
 * anything dangerous, while keeping harmless plain text intact.
 *
 * Example:
 *   Input:  "<script>alert('x')</script>Buy groceries"
 *   Output: "Buy groceries"
 *
 *   Input:  "Buy groceries"         <- clean input
 *   Output: "Buy groceries"         <- unchanged
 */
@Service
public class SanitizationService {

    private static final Logger log = LoggerFactory.getLogger(SanitizationService.class);

    // AntiSamy uses a Policy file to decide what HTML is "safe" to keep.
    // "antisamy-slashdot.xml" is the STRICTEST bundled policy — allows almost
    // nothing. Perfect for plain-text fields like title/description where we
    // never want any HTML at all.
    //
    // Other bundled options (less strict, allow more formatting):
    //   antisamy-myspace.xml  — allows some formatting tags like <b>, <i>
    //   antisamy-tinymce.xml  — allows rich text editing content
    //   antisamy.xml          — a general-purpose balanced policy
    private static final String POLICY_FILE = "antisamy-slashdot.xml";

    private final Policy policy;
    private final AntiSamy antiSamy;

    public SanitizationService() {
        Policy loadedPolicy;
        try {
            // Load the policy file from AntiSamy's bundled jar resources
            InputStream policyStream = Policy.class.getResourceAsStream("/" + POLICY_FILE);
            if (policyStream == null) {
                throw new IllegalStateException(
                        "AntiSamy policy file not found on classpath: " + POLICY_FILE);
            }
            loadedPolicy = Policy.getInstance(policyStream);
            log.info("AntiSamy policy '{}' loaded successfully.", POLICY_FILE);
        } catch (Exception e) {
            log.error("Failed to load AntiSamy policy. Sanitization will be disabled.", e);
            loadedPolicy = null;
        }
        this.policy = loadedPolicy;
        this.antiSamy = new AntiSamy();
    }

    /**
     * Sanitizes a single string value.
     *
     * - If the value is null or blank, it is returned as-is (validation handles that).
     * - If sanitization is unavailable (policy failed to load), the original value
     *   is returned with a warning log.
     * - Dangerous HTML/JS is stripped; safe plain text is returned unchanged.
     *
     * @param input the raw user-provided string
     * @return sanitized string safe to store and display
     */
    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        if (policy == null) {
            log.warn("Sanitization skipped — policy not loaded. Returning raw input.");
            return input;
        }

        try {
            CleanResults results = antiSamy.scan(input, policy);

            // Log if anything was stripped, so you can monitor abuse attempts
            if (results.getNumberOfErrors() > 0) {
                log.warn("Sanitization removed potentially dangerous content. " +
                         "Original: '{}', Cleaned: '{}', Issues: {}",
                         input, results.getCleanHTML(), results.getErrorMessages());
            }

            return results.getCleanHTML();

        } catch (Exception e) {
            // If sanitization itself fails, log and return original.
            // Better to store potentially unsafe data than crash the whole request.
            // Tighten this to re-throw if you prefer strict/fail-fast mode.
            log.error("Sanitization failed for input: '{}'. Returning original.", input, e);
            return input;
        }
    }
}
