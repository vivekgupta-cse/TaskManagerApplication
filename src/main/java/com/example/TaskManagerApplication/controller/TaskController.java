package com.example.TaskManagerApplication.controller;

import com.example.TaskManagerApplication.model.Task;
import com.example.TaskManagerApplication.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Tells Spring this class handles web requests
@RequestMapping("/api/tasks") // All URLs in this class start with /api/tasks
@RequiredArgsConstructor // Automatically creates a constructor for all 'final' fields
public class TaskController {
    // 1. Declare the repository as a 'final' field
    private final TaskService taskService; // Inject Service, not Repository!

    @GetMapping // Maps GET requests to this method
    public List<Task> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")           // ← curly braces define the variable
    public Task getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PostMapping
    public Task createTask(@RequestBody Task newTask) {
        return taskService.createTask(newTask);
    }
}