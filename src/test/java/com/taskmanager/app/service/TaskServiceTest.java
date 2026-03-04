package com.taskmanager.app.service;

import com.taskmanager.app.dto.TaskRequestDTO;
import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.exception.DuplicateTaskException;
import com.taskmanager.app.exception.TaskNotFoundException;
import com.taskmanager.app.mapper.TaskMapper;
import com.taskmanager.app.model.Task;
import com.taskmanager.app.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TaskService.
 * <p>
 * Strategy: Pure unit test — all collaborators (repository, mapper, sanitization)
 * are mocked with Mockito. No Spring context is loaded, so tests run in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private SanitizationService sanitizationService;

    @InjectMocks
    private TaskService taskService;

    // -------------------------------------------------------------------------
    // Common test fixtures
    // -------------------------------------------------------------------------

    private Task sampleTask;
    private TaskResponseDTO sampleResponseDTO;
    private TaskRequestDTO sampleRequestDTO;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .id(1L)
                .header("Buy Groceries") // Note: using 'header' as per entity field name
                .description("Milk and eggs")
                .completed(false)
                .deleted(false)
                // Audit fields are omitted; they will be null or can be set if needed
                .build();

        sampleResponseDTO = new TaskResponseDTO();
        sampleResponseDTO.setId(1L);
        sampleResponseDTO.setTitle("Buy Groceries");
        sampleResponseDTO.setDescription("Milk and eggs");
        sampleResponseDTO.setCompleted(false);
        sampleResponseDTO.setCompletionStatus("PENDING");

        sampleRequestDTO = TaskRequestDTO.builder()
                .title("Buy Groceries")
                .description("Milk and eggs")
                .completed(false)
                .build();
    }

    // =========================================================================
    // getAllTasks()
    // =========================================================================
    @Nested
    @DisplayName("getAllTasks()")
    class GetAllTasks {

        @Test
        @DisplayName("returns all tasks as ResponseDTOs")
        void returnsAllTasksAsDTOs() {
            // 1. Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Task task2 = Task.builder()
                    .id(2L)
                    .header("Read Book")    // Maps to the entity field name
                    .description("Java 25")
                    .completed(true)
                    .deleted(false)
                    .build();

            TaskResponseDTO dto2 = new TaskResponseDTO();
            dto2.setId(2L);
            dto2.setTitle("Read Book");
            dto2.setCompleted(true);
            dto2.setCompletionStatus("DONE");

            // Wrap the entities in a Page object
            Page<Task> taskPage = new PageImpl<>(List.of(sampleTask, task2), pageable, 2);

            // Mock must now use Pageable
            when(taskRepository.findAll(any(Pageable.class))).thenReturn(taskPage);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);
            when(taskMapper.toDTO(task2)).thenReturn(dto2);

            // 2. Act
            Page<TaskResponseDTO> result = taskService.getAllTasks(pageable);

            // 3. Assert
            assertThat(result.getContent()).hasSize(2); // Use .getContent() to access the list
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Buy Groceries");
            assertThat(result.getContent().get(1).getTitle()).isEqualTo("Read Book");
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("returns empty list when no tasks exist")
        void returnsEmptyListWhenNoTasks() {
            // 1. Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(taskRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            // 2. Act
            Page<TaskResponseDTO> result = taskService.getAllTasks(pageable);

            // 3. Assert
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // =========================================================================
    // getTaskById()
    // =========================================================================
    @Nested
    @DisplayName("getTaskById()")
    class GetTaskById {

        @Test
        @DisplayName("returns ResponseDTO when task is found")
        void returnsTaskWhenFound() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);

            TaskResponseDTO result = taskService.getTaskById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Buy Groceries");
            assertThat(result.getCompletionStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task is not found")
        void throwsTaskNotFoundExceptionWhenMissing() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(99L))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================================
    // createTask()
    // =========================================================================
    @Nested
    @DisplayName("createTask()")
    class CreateTask {

        @Test
        @DisplayName("creates and returns a task when title is unique")
        void createsTaskWhenTitleIsUnique() {
            // Sanitization returns the input unchanged
            when(sanitizationService.sanitize("Buy Groceries")).thenReturn("Buy Groceries");
            when(sanitizationService.sanitize("Milk and eggs")).thenReturn("Milk and eggs");

            when(taskRepository.existsByHeaderAndCompletedFalse("Buy Groceries")).thenReturn(false);
            when(taskMapper.toEntity(sampleRequestDTO)).thenReturn(sampleTask);
            when(taskRepository.save(sampleTask)).thenReturn(sampleTask);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);

            TaskResponseDTO result = taskService.createTask(sampleRequestDTO);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Buy Groceries");
            verify(taskRepository).save(sampleTask);
        }

        @Test
        @DisplayName("throws DuplicateTaskException when active task with same title exists")
        void throwsDuplicateExceptionWhenTitleAlreadyExists() {
            when(sanitizationService.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(taskRepository.existsByHeaderAndCompletedFalse("Buy Groceries")).thenReturn(true);

            assertThatThrownBy(() -> taskService.createTask(sampleRequestDTO))
                    .isInstanceOf(DuplicateTaskException.class)
                    .hasMessageContaining("active task");

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("sanitizes title and description before saving")
        void sanitizesInputBeforeSaving() {
            String dirtyTitle = "<script>alert('xss')</script>Buy Groceries";
            String cleanTitle = "Buy Groceries";

            TaskRequestDTO dirtyRequest = TaskRequestDTO.builder()
                    .title(dirtyTitle)
                    .description("Safe description")
                    .completed(false)
                    .build();

            when(sanitizationService.sanitize(dirtyTitle)).thenReturn(cleanTitle);
            when(sanitizationService.sanitize("Safe description")).thenReturn("Safe description");

            // After sanitization, the DTO's title is updated to the clean value
            when(taskRepository.existsByHeaderAndCompletedFalse(cleanTitle)).thenReturn(false);
            when(taskMapper.toEntity(dirtyRequest)).thenReturn(sampleTask);
            when(taskRepository.save(sampleTask)).thenReturn(sampleTask);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);

            taskService.createTask(dirtyRequest);

            // Verify sanitize was called on the dirty input
            verify(sanitizationService).sanitize(dirtyTitle);
            verify(sanitizationService).sanitize("Safe description");
            // After sanitization the DTO's title must be the clean version
            assertThat(dirtyRequest.getTitle()).isEqualTo(cleanTitle);
        }

        @Test
        @DisplayName("sanitizes null description without error")
        void sanitizesNullDescriptionWithoutError() {
            TaskRequestDTO requestWithNullDesc = TaskRequestDTO.builder()
                    .title("Buy Groceries")
                    .description(null)
                    .completed(false)
                    .build();

            when(sanitizationService.sanitize("Buy Groceries")).thenReturn("Buy Groceries");
            when(sanitizationService.sanitize(null)).thenReturn(null);
            when(taskRepository.existsByHeaderAndCompletedFalse("Buy Groceries")).thenReturn(false);
            when(taskMapper.toEntity(requestWithNullDesc)).thenReturn(sampleTask);
            when(taskRepository.save(sampleTask)).thenReturn(sampleTask);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);

            assertThatCode(() -> taskService.createTask(requestWithNullDesc))
                    .doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // updateTask()
    // =========================================================================
    @Nested
    @DisplayName("updateTask()")
    class UpdateTask {

        @Test
        @DisplayName("updates all fields of an existing task")
        void updatesExistingTask() {
            TaskRequestDTO updateRequest = TaskRequestDTO.builder()
                    .title("Buy Groceries Updated")
                    .description("Milk, eggs, and bread")
                    .completed(true)
                    .build();

            Task updatedTask = Task.builder()
                    .id(1L)
                    .header("Buy Groceries Updated") // Use 'header' to match the Entity field
                    .description("Milk, eggs, and bread")
                    .completed(true)
                    .deleted(false)
                    .build();
            TaskResponseDTO updatedDTO = new TaskResponseDTO();
            updatedDTO.setId(1L);
            updatedDTO.setTitle("Buy Groceries Updated");
            updatedDTO.setCompleted(true);
            updatedDTO.setCompletionStatus("DONE");

            when(sanitizationService.sanitize("Buy Groceries Updated")).thenReturn("Buy Groceries Updated");
            when(sanitizationService.sanitize("Milk, eggs, and bread")).thenReturn("Milk, eggs, and bread");
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(sampleTask)).thenReturn(updatedTask);
            when(taskMapper.toDTO(updatedTask)).thenReturn(updatedDTO);

            TaskResponseDTO result = taskService.updateTask(1L, updateRequest);

            assertThat(result.getTitle()).isEqualTo("Buy Groceries Updated");
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getCompletionStatus()).isEqualTo("DONE");
            // Verify entity fields were mutated before save
            assertThat(sampleTask.getHeader()).isEqualTo("Buy Groceries Updated");
            assertThat(sampleTask.getDescription()).isEqualTo("Milk, eggs, and bread");
            assertThat(sampleTask.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task to update does not exist")
        void throwsTaskNotFoundWhenUpdatingMissingTask() {
            when(sanitizationService.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(99L, sampleRequestDTO))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining("99");

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("sanitizes input before updating")
        void sanitizesInputBeforeUpdate() {
            String dirtyTitle = "<b>Clean me</b>";
            String cleanTitle = "Clean me";

            TaskRequestDTO dirtyRequest = TaskRequestDTO.builder()
                    .title(dirtyTitle)
                    .description("desc")
                    .completed(false)
                    .build();

            when(sanitizationService.sanitize(dirtyTitle)).thenReturn(cleanTitle);
            when(sanitizationService.sanitize("desc")).thenReturn("desc");
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(sampleTask)).thenReturn(sampleTask);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);

            taskService.updateTask(1L, dirtyRequest);

            assertThat(dirtyRequest.getTitle()).isEqualTo(cleanTitle);
        }
    }

    // =========================================================================
    // deleteTask()
    // =========================================================================
    @Nested
    @DisplayName("deleteTask()")
    class DeleteTask {

        @Test
        @DisplayName("deletes an existing task without error")
        void deletesExistingTask() {
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            doNothing().when(taskRepository).delete(sampleTask);

            assertThatCode(() -> taskService.deleteTask(1L))
                    .doesNotThrowAnyException();

            verify(taskRepository).delete(sampleTask);
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task to delete does not exist")
        void throwsTaskNotFoundWhenDeletingMissingTask() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.deleteTask(99L))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining("99");

            verify(taskRepository, never()).delete(any());
        }
    }
}

