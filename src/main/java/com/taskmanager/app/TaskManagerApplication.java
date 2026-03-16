package com.taskmanager.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication(scanBasePackages = "com.taskmanager")
public class TaskManagerApplication {

    public static void main(String[] args) {
        // Ensure the logs directory exists so Logback file appenders can create files
        try {
            Path logsDir = Path.of("logs");
            if (!Files.exists(logsDir)) {
                Files.createDirectories(logsDir);
            }
        } catch (IOException e) {
            // This runs before Spring logging is configured, so write to stderr if creation fails
            System.err.println("Warning: could not create logs directory: " + e.getMessage());
        }

        SpringApplication.run(TaskManagerApplication.class, args);
    }
}
