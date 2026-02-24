package com.taskmanager.app.controller;

import com.taskmanager.app.dto.TaskRequestDTO;
import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController               // = @Controller + @ResponseBody: auto-converts return values to JSON
@RequestMapping("/api/tasks") // All endpoints in this class are prefixed with /api/tasks
@RequiredArgsConstructor      // Lombok: generates constructor for all 'final' fields
public class TaskController {

    private final TaskService taskService; // Injected by Spring — never call 'new TaskService()'

    // GET /api/tasks → returns all tasks as a JSON array
    @GetMapping
    public List<TaskResponseDTO> getAllTasks() {
        return taskService.getAllTasks();
    }

    // GET /api/tasks/{id} → returns a single task, or 404 if not found
    @GetMapping("/{id}")
    public TaskResponseDTO getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    // POST /api/tasks → creates a new task, returns the saved task with DB-assigned id
    // @Valid triggers @NotBlank / @Size validation on TaskRequestDTO fields
    @PostMapping
    public TaskResponseDTO createTask(@Valid @RequestBody TaskRequestDTO requestDto) {
        return taskService.createTask(requestDto);
    }

    // PUT /api/tasks/{id} → updates an existing task by id
    // @Valid triggers validation on TaskRequestDTO fields
    @PutMapping("/{id}")
    public TaskResponseDTO updateTask(@PathVariable Long id,
                                      @Valid @RequestBody TaskRequestDTO requestDto) {
        return taskService.updateTask(id, requestDto);
    }

    // DELETE /api/tasks/{id} → deletes a task, returns HTTP 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build(); // 204 — success with no response body
    }
}