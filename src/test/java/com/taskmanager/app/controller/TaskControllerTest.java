package com.taskmanager.app.controller;

import com.taskmanager.app.dto.TaskRequestDTO;
import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.exception.DuplicateTaskException;
import com.taskmanager.app.exception.GlobalExceptionHandler;
import com.taskmanager.app.exception.TaskNotFoundException;
import com.taskmanager.app.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pure unit tests for TaskController + GlobalExceptionHandler via standalone MockMvc.
 *
 * No Spring context, no DB, no Flyway — Mockito mocks TaskService entirely.
 * Uses tools.jackson.databind.ObjectMapper (Spring Boot 4.x / Jackson 3.x).
 */
@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private TaskResponseDTO sampleResponse;
    private TaskRequestDTO validRequest;

    private static final String BASE_URL = "/api/tasks";

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();

        sampleResponse = new TaskResponseDTO();
        sampleResponse.setId(1L);
        sampleResponse.setTitle("Buy Groceries");
        sampleResponse.setDescription("Milk and eggs");
        sampleResponse.setCompleted(false);
        sampleResponse.setCompletionStatus("PENDING");

        validRequest = TaskRequestDTO.builder()
                .title("Buy Groceries")
                .description("Milk and eggs")
                .completed(false)
                .build();
    }

    // =========================================================================
    // GET /api/tasks
    // =========================================================================
    @Nested
    @DisplayName("GET /api/tasks")
    class GetAllTasks {

        @Test
        @DisplayName("returns 200 with paged list of tasks — no filters")
        void returns200WithAllTasks() throws Exception {
            TaskResponseDTO dto2 = new TaskResponseDTO();
            dto2.setId(2L);
            dto2.setTitle("Read Book");
            dto2.setCompleted(true);
            dto2.setCompletionStatus("DONE");

            PageImpl<TaskResponseDTO> page = new PageImpl<>(
                    List.of(sampleResponse, dto2), PageRequest.of(0, 10), 2);

            // getAllTasks now takes (String title, Boolean completed, Pageable)
            when(taskService.getAllTasks(isNull(), isNull(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("Buy Groceries"))
                    .andExpect(jsonPath("$.content[0].completionStatus").value("PENDING"))
                    .andExpect(jsonPath("$.content[1].title").value("Read Book"))
                    .andExpect(jsonPath("$.content[1].completionStatus").value("DONE"))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("returns 200 with empty content array when no tasks exist")
        void returns200WithEmptyArray() throws Exception {
            PageImpl<TaskResponseDTO> emptyPage = new PageImpl<>(
                    List.of(), PageRequest.of(0, 10), 0);

            when(taskService.getAllTasks(isNull(), isNull(), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("passes title query param to service")
        void passesTitleFilter() throws Exception {
            PageImpl<TaskResponseDTO> page = new PageImpl<>(
                    List.of(sampleResponse), PageRequest.of(0, 10), 1);

            when(taskService.getAllTasks(eq("Groceries"), isNull(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get(BASE_URL).param("title", "Groceries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(taskService).getAllTasks(eq("Groceries"), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("passes completed query param to service")
        void passesCompletedFilter() throws Exception {
            PageImpl<TaskResponseDTO> page = new PageImpl<>(
                    List.of(sampleResponse), PageRequest.of(0, 10), 1);

            when(taskService.getAllTasks(isNull(), eq(false), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get(BASE_URL).param("completed", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));

            verify(taskService).getAllTasks(isNull(), eq(false), any(Pageable.class));
        }

        @Test
        @DisplayName("returns 500 with safe message for unexpected exceptions")
        void returns500ForUnexpectedException() throws Exception {
            when(taskService.getAllTasks(any(), any(), any(Pageable.class)))
                    .thenThrow(new RuntimeException("DB connection lost"));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value("Something went wrong. Please try again later."));
        }
    }

    // =========================================================================
    // GET /api/tasks/{id}
    // =========================================================================
    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetTaskById {

        @Test
        @DisplayName("returns 200 with task when found")
        void returns200WhenTaskFound() throws Exception {
            when(taskService.getTaskById(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Buy Groceries"))
                    .andExpect(jsonPath("$.completionStatus").value("PENDING"));
        }

        @Test
        @DisplayName("returns 404 when task not found")
        void returns404WhenTaskNotFound() throws Exception {
            when(taskService.getTaskById(99L)).thenThrow(new TaskNotFoundException(99L));

            mockMvc.perform(get(BASE_URL + "/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Task with ID 99 not found"));
        }
    }

    // =========================================================================
    // POST /api/tasks
    // =========================================================================
    @Nested
    @DisplayName("POST /api/tasks")
    class CreateTask {

        @Test
        @DisplayName("returns 200 with created task for valid input")
        void returns200WithCreatedTask() throws Exception {
            when(taskService.createTask(any(TaskRequestDTO.class))).thenReturn(sampleResponse);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Buy Groceries"))
                    .andExpect(jsonPath("$.completed").value(false));
        }

        @Test
        @DisplayName("returns 400 when title is blank")
        void returns400WhenTitleIsBlank() throws Exception {
            TaskRequestDTO bad = TaskRequestDTO.builder()
                    .title("").description("desc").completed(false).build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors.title").exists());
        }

        @Test
        @DisplayName("returns 400 when title is missing entirely")
        void returns400WhenTitleIsMissing() throws Exception {
            String json = "{\"description\":\"desc\",\"completed\":false}";

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.title").value("Title is required"));
        }

        @Test
        @DisplayName("returns 400 when title is too short (< 3 chars)")
        void returns400WhenTitleTooShort() throws Exception {
            TaskRequestDTO bad = TaskRequestDTO.builder()
                    .title("AB").description("desc").completed(false).build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.title").exists());
        }

        @Test
        @DisplayName("returns 400 when completed field is missing")
        void returns400WhenCompletedIsMissing() throws Exception {
            String json = "{\"title\":\"Buy Groceries\",\"description\":\"desc\"}";

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.completed").value("Completion status must be specified"));
        }

        @Test
        @DisplayName("returns 409 when duplicate active task title")
        void returns409WhenDuplicateTitle() throws Exception {
            when(taskService.createTask(any(TaskRequestDTO.class)))
                    .thenThrow(new DuplicateTaskException("You already have an active task with this title!"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error").value("Conflict"))
                    .andExpect(jsonPath("$.message").value("You already have an active task with this title!"));
        }

        @Test
        @DisplayName("returns 400 when description exceeds 500 characters")
        void returns400WhenDescriptionTooLong() throws Exception {
            TaskRequestDTO bad = TaskRequestDTO.builder()
                    .title("Valid Title")
                    .description("A".repeat(501))
                    .completed(false)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.description").exists());
        }
    }

    // =========================================================================
    // PUT /api/tasks/{id}
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/tasks/{id}")
    class UpdateTask {

        @Test
        @DisplayName("returns 200 with updated task")
        void returns200WithUpdatedTask() throws Exception {
            TaskResponseDTO updated = new TaskResponseDTO();
            updated.setId(1L);
            updated.setTitle("Buy Groceries Updated");
            updated.setCompleted(true);
            updated.setCompletionStatus("DONE");

            when(taskService.updateTask(eq(1L), any(TaskRequestDTO.class))).thenReturn(updated);

            TaskRequestDTO updateRequest = TaskRequestDTO.builder()
                    .title("Buy Groceries Updated")
                    .description("More items")
                    .completed(true)
                    .build();

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Buy Groceries Updated"))
                    .andExpect(jsonPath("$.completionStatus").value("DONE"));
        }

        @Test
        @DisplayName("returns 404 when task to update does not exist")
        void returns404WhenTaskNotFound() throws Exception {
            when(taskService.updateTask(eq(99L), any(TaskRequestDTO.class)))
                    .thenThrow(new TaskNotFoundException(99L));

            mockMvc.perform(put(BASE_URL + "/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("returns 400 when update body has blank title")
        void returns400ForInvalidUpdateBody() throws Exception {
            String json = "{\"title\":\"\",\"completed\":false}";

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.title").exists());
        }
    }

    // =========================================================================
    // DELETE /api/tasks/{id}
    // =========================================================================
    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTask {

        @Test
        @DisplayName("returns 204 No Content on successful delete")
        void returns204OnSuccessfulDelete() throws Exception {
            doNothing().when(taskService).deleteTask(1L);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());

            verify(taskService).deleteTask(1L);
        }

        @Test
        @DisplayName("returns 404 when task to delete does not exist")
        void returns404WhenTaskNotFound() throws Exception {
            doThrow(new TaskNotFoundException(99L)).when(taskService).deleteTask(99L);

            mockMvc.perform(delete(BASE_URL + "/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Task with ID 99 not found"));
        }
    }
}
