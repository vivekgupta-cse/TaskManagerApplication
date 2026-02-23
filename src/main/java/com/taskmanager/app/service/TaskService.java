package com.taskmanager.app.service;

import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.exception.TaskNotFoundException;
import com.taskmanager.app.mapper.TaskMapper;
import com.taskmanager.app.model.Task;
import com.taskmanager.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service                   // Marks this as a Spring managed service bean
@RequiredArgsConstructor   // Lombok: generates constructor for all 'final' fields
public class TaskService {

    private final TaskRepository taskRepository; // injected by Spring
    private final TaskMapper taskMapper;         // injected by Spring

    // READ ALL
    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAll()   // SELECT * FROM tasks  → List<Task>
                .stream()
                .map(taskMapper::toDTO)   // Each Task → TaskResponseDTO
                .toList();                // Collect into a new list (safe, defensive copy)
    }

    // READ ONE
    public TaskResponseDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));  // throws 404 if missing
        return taskMapper.toDTO(task);
    }

    // CREATE
    @Transactional  // If anything fails, the whole operation is rolled back
    public TaskResponseDTO createTask(TaskResponseDTO dto) {
        // Convert DTO (with 'title') to Entity (with 'header')
        Task taskEntity = taskMapper.toEntity(dto); // DTO → Entity

        // Save to DB
        Task savedTask = taskRepository.save(taskEntity); // INSERT into DB

        // Convert back to DTO for the response
        return taskMapper.toDTO(savedTask);          // Entity → DTO (with generated id)
    }

    // UPDATE
    @Transactional
    public TaskResponseDTO updateTask(Long id, TaskResponseDTO taskDetailsDto) {
        // 1. Find the existing record (or throw your custom 404 exception)
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));  // 404 if not found

        // 2. Use the Mapper to update the existing entity with new DTO values
        // This is better than writing 10 setters manually!
        existingTask.setHeader(taskDetailsDto.getTitle());
        existingTask.setDescription(taskDetailsDto.getDescription());
        existingTask.setCompleted(taskDetailsDto.isCompleted());

        // 3. Save the updated entity back to Postgres
        Task updatedTask = taskRepository.save(existingTask);

        // 4. Return the result as a DTO
        return taskMapper.toDTO(updatedTask);
    }

    // DELETE
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.delete(task);
    }
}
