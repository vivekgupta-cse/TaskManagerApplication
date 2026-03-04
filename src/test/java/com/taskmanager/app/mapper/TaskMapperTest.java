package com.taskmanager.app.mapper;

import com.taskmanager.app.dto.TaskRequestDTO;
import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.model.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for TaskMapper (MapStruct-generated code).
 * <p>
 * We use @SpringBootTest to let Spring load the MapStruct-generated
 * TaskMapperImpl bean. The mapping logic is generated at compile-time,
 * so we just verify the field mappings are correct.
 * <p>
 * Note: Uses the test application.yaml (H2 in-memory DB) from src/test/resources.
 */
@SpringBootTest
class TaskMapperTest {

    @Autowired
    private TaskMapper taskMapper;

    // =========================================================================
    // toDTO — Task (Entity) → TaskResponseDTO
    // =========================================================================
    @Nested
    @DisplayName("toDTO() — Entity to ResponseDTO")
    class ToDTOTests {

        @Test
        @DisplayName("maps all fields correctly for an incomplete task")
        void mapsAllFieldsForIncompleteTask() {
            Task task = new Task(1L, "Buy Groceries", "Milk and eggs", false, false);

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getTitle()).isEqualTo("Buy Groceries");  // header → title
            assertThat(dto.getDescription()).isEqualTo("Milk and eggs");
            assertThat(dto.isCompleted()).isFalse();
            assertThat(dto.getCompletionStatus()).isEqualTo("PENDING");  // expression mapping
        }

        @Test
        @DisplayName("maps completionStatus to DONE for a completed task")
        void mapsCompletionStatusToDoneForCompletedTask() {
            Task task = new Task(2L, "Read Book", "Java Guide", true, false);

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.isCompleted()).isTrue();
            assertThat(dto.getCompletionStatus()).isEqualTo("DONE");
        }

        @Test
        @DisplayName("maps header (entity field) to title (DTO field)")
        void mapsHeaderToTitle() {
            // The entity field is 'header', but the DTO field is 'title'
            Task task = new Task(3L, "the-header-value", null, false, false);

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.getTitle()).isEqualTo("the-header-value");
        }

        @Test
        @DisplayName("maps null description correctly")
        void mapsNullDescriptionCorrectly() {
            Task task = new Task(4L, "No Description Task", null, false, false);

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.getDescription()).isNull();
        }
    }

    // =========================================================================
    // toEntity — TaskRequestDTO → Task (Entity)
    // =========================================================================
    @Nested
    @DisplayName("toEntity() — RequestDTO to Entity")
    class ToEntityTests {

        @Test
        @DisplayName("maps all fields correctly")
        void mapsAllFieldsCorrectly() {
            TaskRequestDTO dto = TaskRequestDTO.builder()
                    .title("Buy Groceries")
                    .description("Milk and eggs")
                    .completed(false)
                    .build();

            Task task = taskMapper.toEntity(dto);

            assertThat(task.getHeader()).isEqualTo("Buy Groceries");  // title → header
            assertThat(task.getDescription()).isEqualTo("Milk and eggs");
            assertThat(task.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("id is always null (DB generates it — never from client)")
        void idIsAlwaysNull() {
            TaskRequestDTO dto = TaskRequestDTO.builder()
                    .title("Task Without ID")
                    .description("desc")
                    .completed(false)
                    .build();

            Task task = taskMapper.toEntity(dto);

            assertThat(task.getId()).isNull();  // @Mapping(target = "id", ignore = true)
        }

        @Test
        @DisplayName("maps title (DTO) to header (entity field)")
        void mapsTitleToHeader() {
            TaskRequestDTO dto = TaskRequestDTO.builder()
                    .title("the-title-value")
                    .completed(false)
                    .build();

            Task task = taskMapper.toEntity(dto);

            assertThat(task.getHeader()).isEqualTo("the-title-value");
        }

        @Test
        @DisplayName("maps completed=true correctly")
        void mapsCompletedTrueCorrectly() {
            TaskRequestDTO dto = TaskRequestDTO.builder()
                    .title("Done Task")
                    .completed(true)
                    .build();

            Task task = taskMapper.toEntity(dto);

            assertThat(task.isCompleted()).isTrue();
        }
    }
}

