package com.taskmanager.app.controller;

import tools.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TaskController + GlobalExceptionHandler using MockMvc.
 *
 * Uses standalone MockMvc with Mockito (no Spring test slices required).
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
        @DisplayName("returns 200 with list of tasks")
        void returns200WithAllTasks() throws Exception {
            TaskResponseDTO dto2 = new TaskResponseDTO();
            dto2.setId(2L);
            dto2.setTitle("Read Book");
            dto2.setCompleted(true);
            dto2.setCompletionStatus("DONE");

            when(taskService.getAllTasks()).thenReturn(List.of(sampleResponse, dto2));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("Buy Groceries"))
                    .andExpect(jsonPath("$[0].completionStatus").value("PENDING"))
                    .andExpect(jsonPath("$[1].title").value("Read Book"))
                    .andExpect(jsonPath("$[1].completionStatus").value("DONE"));
        }

        @Test
        @DisplayName("returns 200 with empty array when no tasks")
        void returns200WithEmptyArray() throws Exception {
            when(taskService.getAllTasks()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
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
            when(taskService.getTaskById(99L))
                    .thenThrow(new TaskNotFoundException(99L));

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
            TaskRequestDTO badRequest = TaskRequestDTO.builder()
                    .title("")        // @NotBlank violation
                    .description("desc")
                    .completed(false)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors.title").exists());
        }

        @Test
        @DisplayName("returns 400 when title is missing")
        void returns400WhenTitleIsMissing() throws Exception {
            String json = "{\"description\":\"desc\",\"completed\":false}";

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.title").value("Title is required"));
        }

        @Test
        @DisplayName("returns 400 when title is too short")
        void returns400WhenTitleTooShort() throws Exception {
            TaskRequestDTO badRequest = TaskRequestDTO.builder()
                    .title("AB")      // @Size(min=3) violation
                    .description("desc")
                    .completed(false)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
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
            TaskRequestDTO badRequest = TaskRequestDTO.builder()
                    .title("Valid Title")
                    .description("A".repeat(501))  // @Size(max=500) violation
                    .completed(false)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
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
        @DisplayName("returns 400 when update body is invalid")
        void returns400ForInvalidUpdateBody() throws Exception {
            String json = "{\"title\":\"\",\"completed\":false}";   // blank title

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

    // =========================================================================
    // GlobalExceptionHandler — generic 500
    // =========================================================================
    @Nested
    @DisplayName("GlobalExceptionHandler — generic exception")
    class GenericException {

        @Test
        @DisplayName("returns 500 with safe message for unexpected exceptions")
        void returns500ForUnexpectedException() throws Exception {
            when(taskService.getAllTasks()).thenThrow(new RuntimeException("DB connection lost"));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    // Must NOT expose internal details
                    .andExpect(jsonPath("$.message").value("Something went wrong. Please try again later."));
        }
    }
}
