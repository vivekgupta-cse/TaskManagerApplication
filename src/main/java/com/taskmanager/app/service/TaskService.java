package com.taskmanager.app.service;

import com.taskmanager.app.exception.TaskNotFoundException;
import com.taskmanager.app.model.Task;
import com.taskmanager.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional // Ensures the database operation is safe
    public Task createTask(Task task) {
        // Business Rule Example: Capitalize the title before saving
        task.setTitle(task.getTitle().toUpperCase());
        return taskRepository.save(task);
    }
}
