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
            Task task = Task.builder()
                    .id(1L)
                    .header("Buy Groceries")
                    .description("Milk and eggs")
                    .completed(false)
                    .deleted(false)
                    .build();

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
            Task task = Task.builder()
                    .id(2L)
                    .header("Read Book")
                    .description("Java Guide")
                    .completed(true)
                    .deleted(false)
                    .build();

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.isCompleted()).isTrue();
            assertThat(dto.getCompletionStatus()).isEqualTo("DONE");
        }

        @Test
        @DisplayName("maps header (entity field) to title (DTO field)")
        void mapsHeaderToTitle() {
            // The entity field is 'header', but the DTO field is 'title'
            Task task = Task.builder()
                    .id(3L)
                    .header("the-header-value")
                    .completed(false)
                    .deleted(false)
                    .build();

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.getTitle()).isEqualTo("the-header-value");
        }

        @Test
        @DisplayName("maps null description correctly")
        void mapsNullDescriptionCorrectly() {
            Task task = Task.builder()
                    .id(4L)
                    .header("No Description Task")
                    .description(null)
                    .completed(false)
                    .deleted(false)
                    .build();

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

