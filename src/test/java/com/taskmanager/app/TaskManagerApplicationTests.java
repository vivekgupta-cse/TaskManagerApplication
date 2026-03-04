package com.taskmanager.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

class TaskManagerApplicationTests {

    @Test
    void mainStartsAndStops() {
        SpringApplication app = new SpringApplication(TaskManagerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = app.run()) {
            // Context starts and closes cleanly; main class is covered.
        }
    }
}
