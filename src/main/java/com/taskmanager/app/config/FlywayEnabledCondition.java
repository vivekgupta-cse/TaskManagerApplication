package com.taskmanager.app.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that enables Flyway when either "spring.flyway.enabled" or "flyway.enabled"
 * is explicitly set to true. If neither property is present, Flyway will be disabled
 * to avoid running migrations during unit tests unless tests opt in.
 */
public class FlywayEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String springFlyway = context.getEnvironment().getProperty("spring.flyway.enabled");
        String flyway = context.getEnvironment().getProperty("flyway.enabled");

        if (springFlyway != null) {
            return Boolean.parseBoolean(springFlyway);
        }
        if (flyway != null) {
            return Boolean.parseBoolean(flyway);
        }
        // Default to true so Flyway runs in normal application environments
        // unless explicitly disabled (tests set spring.flyway.enabled=false).
        return true;
    }
}

