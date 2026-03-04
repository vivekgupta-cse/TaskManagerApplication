package com.taskmanager.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // This is the "on" switch for auditing
public class JpaConfig {
}
