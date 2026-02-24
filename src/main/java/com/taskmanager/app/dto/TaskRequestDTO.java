package com.taskmanager.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TaskRequestDTO.java — what clients SEND (POST / PUT request body)
// Does NOT have: id (DB generates it), completionStatus (server computes it)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    // This ensures 'completed' is always handled correctly
    @NotNull(message = "Completion status must be specified")
    private boolean completed;  // defaults to false (boolean primitive default)
}
