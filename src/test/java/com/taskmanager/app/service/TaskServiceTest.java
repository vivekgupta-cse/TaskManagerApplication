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
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for TaskService.
 *
 * All collaborators (repository, mapper, sanitizationService) are mocked.
 * No Spring context, no DB, no Flyway — runs in milliseconds.
 */
@SuppressWarnings("unchecked") // safe: Mockito any(Specification.class) requires raw type at runtime
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private SanitizationService sanitizationService;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;
    private TaskResponseDTO sampleResponseDTO;
    private TaskRequestDTO sampleRequestDTO;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .id(1L)
                .header("Buy Groceries")  // entity field is 'header', not 'title'
                .description("Milk and eggs")
                .completed(false)
                .deleted(false)
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
    // getAllTasks(String title, Boolean completed, Pageable pageable)
    // =========================================================================
    @Nested
    @DisplayName("getAllTasks()")
    class GetAllTasks {

        @Test
        @DisplayName("returns paged results mapped to ResponseDTOs")
        void returnsPagedResultsAsDTOs() {
            Pageable pageable = PageRequest.of(0, 10);
            Task task2 = Task.builder().id(2L).header("Read Book")
                    .completed(true).deleted(false).build();
            TaskResponseDTO dto2 = new TaskResponseDTO();
            dto2.setId(2L); dto2.setTitle("Read Book");
            dto2.setCompleted(true); dto2.setCompletionStatus("DONE");

            Page<Task> taskPage = new PageImpl<>(List.of(sampleTask, task2), pageable, 2);

            // Service uses Specification-based findAll, not plain findAll(Pageable)
            when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(taskPage);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);
            when(taskMapper.toDTO(task2)).thenReturn(dto2);

            Page<TaskResponseDTO> result = taskService.getAllTasks(null, null, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Buy Groceries");
            assertThat(result.getContent().get(1).getTitle()).isEqualTo("Read Book");
            assertThat(result.getTotalElements()).isEqualTo(2);
            // Spec-based findAll was called, never the plain one
            verify(taskRepository).findAll(any(Specification.class), eq(pageable));
            verify(taskRepository, never()).findAll(eq(pageable));
        }

        @Test
        @DisplayName("returns empty page when no tasks exist")
        void returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<TaskResponseDTO> result = taskService.getAllTasks(null, null, pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("passes title and completed filters through to specification")
        void passesFiltersToSpec() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> page = new PageImpl<>(List.of(sampleTask), pageable, 1);
            when(taskRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);

            // With title + completed filters — service still delegates to Specification
            Page<TaskResponseDTO> result = taskService.getAllTasks("Buy", false, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        }
    }

    // =========================================================================
    // getTaskById(Long id)
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
        @DisplayName("throws TaskNotFoundException when task does not exist")
        void throwsWhenNotFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(99L))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================================
    // createTask(TaskRequestDTO)
    // =========================================================================
    @Nested
    @DisplayName("createTask()")
    class CreateTask {

        @Test
        @DisplayName("creates and returns a task when title is unique")
        void createsTaskSuccessfully() {
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
        void throwsDuplicateException() {
            when(sanitizationService.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(taskRepository.existsByHeaderAndCompletedFalse("Buy Groceries")).thenReturn(true);

            assertThatThrownBy(() -> taskService.createTask(sampleRequestDTO))
                    .isInstanceOf(DuplicateTaskException.class)
                    .hasMessageContaining("active task");

            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("sanitizes title and description before saving")
        void sanitizesInputBeforeSaving() {
            String dirtyTitle = "<script>alert('xss')</script>Buy Groceries";
            String cleanTitle = "Buy Groceries";

            TaskRequestDTO dirtyRequest = TaskRequestDTO.builder()
                    .title(dirtyTitle).description("Safe desc").completed(false).build();

            when(sanitizationService.sanitize(dirtyTitle)).thenReturn(cleanTitle);
            when(sanitizationService.sanitize("Safe desc")).thenReturn("Safe desc");
            when(taskRepository.existsByHeaderAndCompletedFalse(cleanTitle)).thenReturn(false);
            when(taskMapper.toEntity(dirtyRequest)).thenReturn(sampleTask);
            when(taskRepository.save(sampleTask)).thenReturn(sampleTask);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);

            taskService.createTask(dirtyRequest);

            verify(sanitizationService).sanitize(dirtyTitle);
            verify(sanitizationService).sanitize("Safe desc");
            // DTO title is mutated to the clean version before duplicate check
            assertThat(dirtyRequest.getTitle()).isEqualTo(cleanTitle);
        }

        @Test
        @DisplayName("handles null description without error")
        void handlesNullDescription() {
            TaskRequestDTO req = TaskRequestDTO.builder()
                    .title("Buy Groceries").description(null).completed(false).build();

            when(sanitizationService.sanitize("Buy Groceries")).thenReturn("Buy Groceries");
            when(sanitizationService.sanitize(null)).thenReturn(null);
            when(taskRepository.existsByHeaderAndCompletedFalse("Buy Groceries")).thenReturn(false);
            when(taskMapper.toEntity(req)).thenReturn(sampleTask);
            when(taskRepository.save(sampleTask)).thenReturn(sampleTask);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);

            assertThatCode(() -> taskService.createTask(req)).doesNotThrowAnyException();
        }
    }

    // =========================================================================
    // updateTask(Long id, TaskRequestDTO)
    // =========================================================================
    @Nested
    @DisplayName("updateTask()")
    class UpdateTask {

        @Test
        @DisplayName("updates all fields of an existing task")
        void updatesExistingTask() {
            TaskRequestDTO updateReq = TaskRequestDTO.builder()
                    .title("Buy Groceries Updated")
                    .description("Milk, eggs, and bread")
                    .completed(true)
                    .build();

            Task updatedTask = Task.builder().id(1L)
                    .header("Buy Groceries Updated")
                    .description("Milk, eggs, and bread")
                    .completed(true).deleted(false).build();

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

            TaskResponseDTO result = taskService.updateTask(1L, updateReq);

            assertThat(result.getTitle()).isEqualTo("Buy Groceries Updated");
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getCompletionStatus()).isEqualTo("DONE");
            // Verify the entity was mutated in-place before save
            assertThat(sampleTask.getHeader()).isEqualTo("Buy Groceries Updated");
            assertThat(sampleTask.getDescription()).isEqualTo("Milk, eggs, and bread");
            assertThat(sampleTask.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task to update does not exist")
        void throwsWhenNotFound() {
            when(sanitizationService.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(99L, sampleRequestDTO))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining("99");

            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        @DisplayName("sanitizes input before updating")
        void sanitizesBeforeUpdate() {
            TaskRequestDTO dirtyReq = TaskRequestDTO.builder()
                    .title("<b>Clean me</b>").description("desc").completed(false).build();

            when(sanitizationService.sanitize("<b>Clean me</b>")).thenReturn("Clean me");
            when(sanitizationService.sanitize("desc")).thenReturn("desc");
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(sampleTask)).thenReturn(sampleTask);
            when(taskMapper.toDTO(sampleTask)).thenReturn(sampleResponseDTO);

            taskService.updateTask(1L, dirtyReq);

            assertThat(dirtyReq.getTitle()).isEqualTo("Clean me");
        }
    }

    // =========================================================================
    // deleteTask(Long id)
    // =========================================================================
    @Nested
    @DisplayName("deleteTask()")
    class DeleteTask {

        @Test
        @DisplayName("soft-deletes an existing task without error")
        void deletesExistingTask() {
              when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
              when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

              assertThatCode(() -> taskService.deleteTask(1L)).doesNotThrowAnyException();

              // Soft-delete uses save() to persist the deleted flag
              verify(taskRepository).save(argThat(t -> t.isDeleted()));
        }

        @Test
        @DisplayName("throws TaskNotFoundException when task to delete does not exist")
        void throwsWhenNotFound() {
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.deleteTask(99L))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining("99");

            // Specify Task.class to avoid ambiguity with JpaSpecificationExecutor.delete(Spec)
            verify(taskRepository, never()).delete(any(Task.class));
        }
    }
}

