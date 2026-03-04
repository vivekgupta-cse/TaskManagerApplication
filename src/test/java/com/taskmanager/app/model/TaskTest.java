package com.taskmanager.app.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Task entity.
 *
 * Covers Lombok-generated code: @Builder, @Builder.Default, @Getter, @Setter,
 * @NoArgsConstructor, @AllArgsConstructor.
 */
class TaskTest {

    @Test
    @DisplayName("builder creates entity with all fields")
    void builderCreatesEntity() {
        LocalDateTime now = LocalDateTime.now();

        Task task = Task.builder()
                .id(1L)
                .header("Buy Groceries")
                .description("Milk and eggs")
                .completed(false)
                .deleted(true)
                .createdAt(now)
                .lastModifiedAt(now)
                .deletedAt(now)
                .build();

        assertThat(task.getId()).isEqualTo(1L);
        assertThat(task.getHeader()).isEqualTo("Buy Groceries");
        assertThat(task.getDescription()).isEqualTo("Milk and eggs");
        assertThat(task.isCompleted()).isFalse();
        assertThat(task.isDeleted()).isTrue();
        assertThat(task.getCreatedAt()).isEqualTo(now);
        assertThat(task.getLastModifiedAt()).isEqualTo(now);
        assertThat(task.getDeletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("@Builder.Default sets deleted to false when not explicitly set")
    void builderDefaultDeletedIsFalse() {
        Task task = Task.builder()
                .header("Test")
                .build();

        assertThat(task.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("no-arg constructor creates entity with default values")
    void noArgConstructor() {
        Task task = new Task();

        assertThat(task.getId()).isNull();
        assertThat(task.getHeader()).isNull();
        assertThat(task.getDescription()).isNull();
        assertThat(task.isCompleted()).isFalse();
        assertThat(task.isDeleted()).isFalse(); // initializer = false
        assertThat(task.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("setters mutate entity fields correctly")
    void settersMutateFields() {
        Task task = new Task();
        LocalDateTime now = LocalDateTime.now();

        task.setId(5L);
        task.setHeader("Updated Header");
        task.setDescription("Updated Desc");
        task.setCompleted(true);
        task.setDeleted(true);
        task.setCreatedAt(now);
        task.setLastModifiedAt(now);
        task.setDeletedAt(now);

        assertThat(task.getId()).isEqualTo(5L);
        assertThat(task.getHeader()).isEqualTo("Updated Header");
        assertThat(task.getDescription()).isEqualTo("Updated Desc");
        assertThat(task.isCompleted()).isTrue();
        assertThat(task.isDeleted()).isTrue();
        assertThat(task.getCreatedAt()).isEqualTo(now);
        assertThat(task.getLastModifiedAt()).isEqualTo(now);
        assertThat(task.getDeletedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("all-args constructor sets every field")
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();

        Task task = new Task(1L, "Header", "Desc", true, true, now, now, now);

        assertThat(task.getId()).isEqualTo(1L);
        assertThat(task.getHeader()).isEqualTo("Header");
        assertThat(task.getDescription()).isEqualTo("Desc");
        assertThat(task.isCompleted()).isTrue();
        assertThat(task.isDeleted()).isTrue();
        assertThat(task.getCreatedAt()).isEqualTo(now);
        assertThat(task.getLastModifiedAt()).isEqualTo(now);
        assertThat(task.getDeletedAt()).isEqualTo(now);
    }
}

