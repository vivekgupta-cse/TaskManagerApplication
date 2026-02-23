package com.taskmanager.app.controller;

import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // = @Controller + @ResponseBody (auto-convert return value to JSON), Tells Spring this class handles web requests
@RequestMapping("/api/tasks") // All URLs in this class start with /api/tasks
@RequiredArgsConstructor // Automatically creates a constructor for all 'final' fields
public class TaskController {
    // 1. Declare the repository as a 'final' field
    private final TaskService taskService; // Inject Service, not Repository!

    // GET /api/tasks  → returns all tasks as JSON array
    @GetMapping // Maps GET requests to this method
    public List<TaskResponseDTO> getAllTasks() {
        return taskService.getAllTasks();
    }

    // GET /api/tasks/5  → returns single task or 404
    @GetMapping("/{id}")           // ← curly braces define the variable
    public TaskResponseDTO getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id); // Ensure this method in Service also returns DTO
    }

    // POST /api/tasks  → creates a new task, returns the saved task (with DB-assigned id)
    @PostMapping
    // Use TaskResponseDTO as the input type!
    public TaskResponseDTO createTask(@RequestBody TaskResponseDTO newTaskDto) {
        return taskService.createTask(newTaskDto);
    }

    // PUT /api/tasks/5  → updates existing task
    @PutMapping("/{id}")
    // Use TaskResponseDTO as the input type!
    public TaskResponseDTO updateTask(@PathVariable Long id, @RequestBody TaskResponseDTO taskDetailsDto) {
        return taskService.updateTask(id, taskDetailsDto);
    }

    // DELETE /api/tasks/5  → deletes a task, returns HTTP 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        // Return 204 No Content - the standard for successful deletes
        return ResponseEntity.noContent().build();
    }
}