package com.taskmanager.app.mapper;

import com.taskmanager.app.dto.TaskRequestDTO;
import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.model.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TaskMapper (MapStruct-generated code).
 *
 * We only need the MapStruct mapper bean — no DB, no JPA, no Flyway.
 * Auto-configurations for DataSource, JPA, and Flyway are disabled via
 * properties so this test runs without any database connection at all.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
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
            assertThat(dto.getTitle()).isEqualTo("Buy Groceries");
            assertThat(dto.getDescription()).isEqualTo("Milk and eggs");
            assertThat(dto.isCompleted()).isFalse();
            assertThat(dto.getCompletionStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("maps completionStatus to DONE for a completed task")
        void mapsCompletionStatusToDoneForCompletedTask() {
            Task task = Task.builder()
                    .id(2L).header("Read Book").description("Java Guide")
                    .completed(true).deleted(false).build();

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.isCompleted()).isTrue();
            assertThat(dto.getCompletionStatus()).isEqualTo("DONE");
        }

        @Test
        @DisplayName("maps header (entity field) to title (DTO field)")
        void mapsHeaderToTitle() {
            Task task = Task.builder()
                    .id(3L).header("the-header-value").completed(false).deleted(false).build();

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.getTitle()).isEqualTo("the-header-value");
        }

        @Test
        @DisplayName("maps null description correctly")
        void mapsNullDescriptionCorrectly() {
            Task task = Task.builder()
                    .id(4L).header("No Desc Task").description(null)
                    .completed(false).deleted(false).build();

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.getDescription()).isNull();
        }

        @Test
        @DisplayName("audit fields createdAt and lastModifiedAt are mapped")
        void mapsAuditFields() {
            LocalDateTime now = LocalDateTime.now();
            Task task = Task.builder()
                    .id(5L).header("Audit Task").completed(false).deleted(false)
                    .createdAt(now).lastModifiedAt(now).build();

            TaskResponseDTO dto = taskMapper.toDTO(task);

            assertThat(dto.getCreatedAt()).isEqualTo(now);
            assertThat(dto.getLastModifiedAt()).isEqualTo(now);
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
                    .title("Buy Groceries").description("Milk and eggs").completed(false).build();

            Task task = taskMapper.toEntity(dto);

            assertThat(task.getHeader()).isEqualTo("Buy Groceries");
            assertThat(task.getDescription()).isEqualTo("Milk and eggs");
            assertThat(task.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("id is always null — DB generates it, never from client")
        void idIsAlwaysNull() {
            TaskRequestDTO dto = TaskRequestDTO.builder()
                    .title("Task Without ID").description("desc").completed(false).build();

            Task task = taskMapper.toEntity(dto);

            assertThat(task.getId()).isNull();
        }

        @Test
        @DisplayName("maps title (DTO) to header (entity field)")
        void mapsTitleToHeader() {
            TaskRequestDTO dto = TaskRequestDTO.builder()
                    .title("the-title-value").completed(false).build();

            Task task = taskMapper.toEntity(dto);

            assertThat(task.getHeader()).isEqualTo("the-title-value");
        }

        @Test
        @DisplayName("maps completed=true correctly")
        void mapsCompletedTrueCorrectly() {
            TaskRequestDTO dto = TaskRequestDTO.builder()
                    .title("Done Task").completed(true).build();

            Task task = taskMapper.toEntity(dto);

            assertThat(task.isCompleted()).isTrue();
        }
    }
}
