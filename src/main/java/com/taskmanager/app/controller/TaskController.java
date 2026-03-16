package com.taskmanager.app.controller;

import com.taskmanager.app.dto.TaskRequestDTO;
import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.service.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController               // = @Controller + @ResponseBody: auto-converts return values to JSON
@RequestMapping("/api/tasks") // All endpoints in this class are prefixed with /api/tasks
@RequiredArgsConstructor      // Lombok: generates constructor for all 'final' fields
@Tag(name = "Tasks", description = "CRUD operations for tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService; // Injected by Spring — never call 'new TaskService()'

    // GET /api/tasks → returns all tasks as a JSON array
    @GetMapping
    public ResponseEntity<Page<TaskResponseDTO>> getAllTasks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean completed,
            @PageableDefault(size = 10, sort = "header") Pageable pageable) {

        // We pass the filters + the pageable object to the service
        log.debug("getAllTasks called with title='{}', completed={}, pageable={}", title, completed, pageable);
        Page<TaskResponseDTO> tasks = taskService.getAllTasks(title, completed, pageable);
        log.debug("getAllTasks returning {} entries (page {}).", tasks.getNumberOfElements(), tasks.getNumber());
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/{id} → returns a single task, or 404 if not found
    @GetMapping("/{id}")
    public TaskResponseDTO getTaskById(@PathVariable Long id) {
        log.debug("getTaskById called with id={}", id);
        TaskResponseDTO dto = taskService.getTaskById(id);
        log.debug("getTaskById returning: {}", dto);
        return dto;
    }

    // GET /api/tasks/search?q=Grocery → fuzzy search: matches "Groceries", "Grocery", "Buy Groceries", etc.
    // Uses PostgreSQL pg_trgm similarity — tolerates typos and partial words.
    @GetMapping("/search")
    public ResponseEntity<Page<TaskResponseDTO>> searchTasks(
            @RequestParam String title,
            @PageableDefault(size = 10, sort = "header") Pageable pageable) {
        log.debug("searchTasks fuzzy search for '{}' pageable={}", title, pageable);
        Page<TaskResponseDTO> page = taskService.searchTasksByFuzzyTitle(title, pageable);
        log.debug("searchTasks found {} results", page.getNumberOfElements());
        return ResponseEntity.ok(page);
    }

    // GET /api/tasks?header=Buy%20Groceries → exact match by task title (header field)
    @GetMapping(params = "title")
    public TaskResponseDTO getTaskByTitle(@RequestParam String title) {
        log.debug("getTaskByTitle called with title={}", title);
        TaskResponseDTO dto = taskService.getTaskByTitle(title);
        log.debug("getTaskByTitle returning {}", dto);
        return dto;
    }

    // POST /api/tasks → creates a new task, returns the saved task with DB-assigned id
    // @Valid triggers @NotBlank / @Size validation on TaskRequestDTO fields
    @PostMapping
    public TaskResponseDTO createTask(@Valid @RequestBody TaskRequestDTO requestDto) {
        log.debug("createTask called with payload={}", requestDto);
        TaskResponseDTO created = taskService.createTask(requestDto);
        log.info("Task created with id={}", created.getId());
        return created;
    }

    // PUT /api/tasks/{id} → updates an existing task by id
    // @Valid triggers validation on TaskRequestDTO fields
    @PutMapping("/{id}")
    public TaskResponseDTO updateTask(@PathVariable Long id,
                                      @Valid @RequestBody TaskRequestDTO requestDto) {
        log.debug("updateTask called id={}, payload={}", id, requestDto);
        TaskResponseDTO updated = taskService.updateTask(id, requestDto);
        log.info("Task updated id={}", updated.getId());
        return updated;
    }

    // DELETE /api/tasks/{id} → deletes a task, returns HTTP 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        log.debug("deleteTask called for id={}", id);
        taskService.deleteTask(id);
        log.info("Task marked deleted id={}", id);
        return ResponseEntity.noContent().build(); // 204 — success with no response body
    }
}