package com.taskmanager.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TaskRequestDTO.java — what clients SEND (POST / PUT request body)
// Does NOT have: id (DB generates it), completionStatus (server computes it)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    // Boolean wrapper (not primitive boolean) — so @NotNull actually works.
    // If client omits this field entirely, it stays null and @NotNull triggers a 400 error.
    // With primitive 'boolean', it silently defaults to false and @NotNull has no effect.
    @NotNull(message = "Completion status must be specified")
    private Boolean completed;
}
