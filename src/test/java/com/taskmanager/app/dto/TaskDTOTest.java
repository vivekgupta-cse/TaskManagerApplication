package com.taskmanager.app.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TaskRequestDTO and TaskResponseDTO.
 *
 * Covers Lombok-generated code: @Builder, @Getter, @Setter, @Data (equals, hashCode, toString).
 * JaCoCo counts Lombok-generated methods, so we must exercise them to avoid coverage drops.
 */
class TaskDTOTest {

    // =========================================================================
    // TaskRequestDTO
    // =========================================================================
    @Nested
    @DisplayName("TaskRequestDTO")
    class RequestDTO {

        @Test
        @DisplayName("builder creates instance with all fields")
        void builderCreatesInstance() {
            TaskRequestDTO dto = TaskRequestDTO.builder()
                    .title("Buy Groceries")
                    .description("Milk and eggs")
                    .completed(false)
                    .build();

            assertThat(dto.getTitle()).isEqualTo("Buy Groceries");
            assertThat(dto.getDescription()).isEqualTo("Milk and eggs");
            assertThat(dto.getCompleted()).isFalse();
        }

        @Test
        @DisplayName("no-arg constructor creates instance with null fields")
        void noArgConstructor() {
            TaskRequestDTO dto = new TaskRequestDTO();

            assertThat(dto.getTitle()).isNull();
            assertThat(dto.getDescription()).isNull();
            assertThat(dto.getCompleted()).isNull();
        }

        @Test
        @DisplayName("all-arg constructor sets all fields")
        void allArgConstructor() {
            TaskRequestDTO dto = new TaskRequestDTO("Title", "Desc", true);

            assertThat(dto.getTitle()).isEqualTo("Title");
            assertThat(dto.getDescription()).isEqualTo("Desc");
            assertThat(dto.getCompleted()).isTrue();
        }

        @Test
        @DisplayName("setters mutate fields correctly")
        void settersMutateFields() {
            TaskRequestDTO dto = new TaskRequestDTO();
            dto.setTitle("New Title");
            dto.setDescription("New Desc");
            dto.setCompleted(true);

            assertThat(dto.getTitle()).isEqualTo("New Title");
            assertThat(dto.getDescription()).isEqualTo("New Desc");
            assertThat(dto.getCompleted()).isTrue();
        }
    }

    // =========================================================================
    // TaskResponseDTO
    // =========================================================================
    @Nested
    @DisplayName("TaskResponseDTO")
    class ResponseDTO {

        @Test
        @DisplayName("@Data generates working getters and setters")
        void gettersAndSetters() {
            TaskResponseDTO dto = new TaskResponseDTO();
            dto.setId(1L);
            dto.setTitle("Buy Groceries");
            dto.setDescription("Milk");
            dto.setCompleted(false);
            dto.setCompletionStatus("PENDING");
            dto.setCreatedAt(java.time.LocalDateTime.now());
            dto.setLastModifiedAt(java.time.LocalDateTime.now());

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getTitle()).isEqualTo("Buy Groceries");
            assertThat(dto.getDescription()).isEqualTo("Milk");
            assertThat(dto.isCompleted()).isFalse();
            assertThat(dto.getCompletionStatus()).isEqualTo("PENDING");
            assertThat(dto.getCreatedAt()).isNotNull();
            assertThat(dto.getLastModifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("equals and hashCode work correctly for @Data")
        void equalsAndHashCode() {
            TaskResponseDTO dto1 = new TaskResponseDTO();
            dto1.setId(1L);
            dto1.setTitle("Buy Groceries");
            dto1.setCompleted(false);

            TaskResponseDTO dto2 = new TaskResponseDTO();
            dto2.setId(1L);
            dto2.setTitle("Buy Groceries");
            dto2.setCompleted(false);

            assertThat(dto1).isEqualTo(dto2);
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());
        }

        @Test
        @DisplayName("toString contains field values")
        void toStringContainsFields() {
            TaskResponseDTO dto = new TaskResponseDTO();
            dto.setId(1L);
            dto.setTitle("Test");

            String str = dto.toString();
            assertThat(str).contains("id=1");
            assertThat(str).contains("title=Test");
        }

        @Test
        @DisplayName("different objects are not equal")
        void differentObjectsNotEqual() {
            TaskResponseDTO dto1 = new TaskResponseDTO();
            dto1.setId(1L);
            dto1.setTitle("A");

            TaskResponseDTO dto2 = new TaskResponseDTO();
            dto2.setId(2L);
            dto2.setTitle("B");

            assertThat(dto1).isNotEqualTo(dto2);
        }
    }
}

