package com.taskmanager.app.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret:#{null}}")
    private String configuredSecret;

    private String activeSecret;

    @PostConstruct
    public void init() {
        if (configuredSecret == null || configuredSecret.isEmpty()) {
            // Generate the key only once during startup
            byte[] keyBytes = new byte[32];
            new SecureRandom().nextBytes(keyBytes);
            this.activeSecret = Base64.getEncoder().encodeToString(keyBytes);
        } else {
            this.activeSecret = configuredSecret;
        }
    }

    public String getSecret() {
        return this.activeSecret;
    }
}