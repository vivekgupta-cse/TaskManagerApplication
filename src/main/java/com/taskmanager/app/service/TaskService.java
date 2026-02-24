package com.taskmanager.app.service;

import com.taskmanager.app.dto.TaskRequestDTO;
import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.exception.DuplicateTaskException;
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

    // CREATE — accepts TaskRequestDTO (no id, no completionStatus from client)
    @Transactional  // If anything fails, the whole operation is rolled back
    public TaskResponseDTO createTask(TaskRequestDTO requestDto) {
        // Business Validation
        if (taskRepository.existsByHeaderAndCompletedFalse(requestDto.getTitle())) {
            throw new DuplicateTaskException("You already have an active task with this title!");
        }
        Task taskEntity = taskMapper.toEntity(requestDto); // RequestDTO → Entity (id is null, DB assigns it)
        Task savedTask  = taskRepository.save(taskEntity); // INSERT into DB
        return taskMapper.toDTO(savedTask);                // Entity → ResponseDTO (with DB-generated id)
    }

    // UPDATE — accepts TaskRequestDTO (client cannot change the id via request body)
    @Transactional
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDto) {
        // 1. Find the existing record (or throw 404)
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        // 2. Apply the new values from the request onto the existing entity
        existingTask.setHeader(requestDto.getTitle());
        existingTask.setDescription(requestDto.getDescription());
        existingTask.setCompleted(requestDto.isCompleted());

        // 3. Save updated entity back to DB
        Task updatedTask = taskRepository.save(existingTask);

        // 4. Return as ResponseDTO
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
