package com.taskmanager.app.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data                       // Lombok: getters, setters, toString, equals, hashCode
public class TaskResponseDTO {
    private Long id;
    private String title;            // Named "title" (matches what clients expect)
    private String description;
    private boolean completed;
    private String completionStatus; // EXTRA field — does NOT exist in the DB!

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    // We usually don't send deletedAt because deleted tasks are filtered out!
}