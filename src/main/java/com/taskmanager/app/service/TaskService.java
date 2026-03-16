package com.taskmanager.app.service;

import com.taskmanager.app.dto.TaskRequestDTO;
import com.taskmanager.app.dto.TaskResponseDTO;
import com.taskmanager.app.exception.DuplicateTaskException;
import com.taskmanager.app.exception.TaskNotFoundException;
import com.taskmanager.app.mapper.TaskMapper;
import com.taskmanager.app.model.Task;
import com.taskmanager.app.repository.TaskRepository;
import com.taskmanager.app.specification.TaskSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Service                   // Marks this as a Spring managed service bean
@RequiredArgsConstructor   // Lombok: generates constructor for all 'final' fields
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;           // injected by Spring
    private final TaskMapper taskMapper;                   // injected by Spring
    private final SanitizationService sanitizationService; // injected by Spring

    // READ ALL
    public Page<TaskResponseDTO> getAllTasks(String title, Boolean completed, Pageable pageable) {
        log.debug("getAllTasks service called title='{}' completed={} pageable={}", title, completed, pageable);
        Specification<Task> spec = Specification.where(TaskSpecifications.hasTitle(title))
                .and(TaskSpecifications.isCompleted(completed));

        Page<TaskResponseDTO> result = taskRepository.findAll(spec, pageable).map(taskMapper::toDTO);
        log.debug("getAllTasks returning {} records (page {})", result.getNumberOfElements(), result.getNumber());
        return result;
    }

    // READ ONE
    public TaskResponseDTO getTaskById(Long id) {
        log.debug("getTaskById service called id={}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));  // throws 404 if missing
        TaskResponseDTO dto = taskMapper.toDTO(task);
        log.debug("getTaskById returning {}", dto);
        return dto;
    }

    public TaskResponseDTO getTaskByTitle(String title) {
        log.debug("getTaskByTitle service called title={}", title);
        Task task = taskRepository.findByHeader(title)
                .orElseThrow(() -> new TaskNotFoundException(title));  // throws 404 if missing
        TaskResponseDTO dto = taskMapper.toDTO(task);
        log.debug("getTaskByTitle returning {}", dto);
        return dto;
    }

    // FUZZY SEARCH — returns all tasks whose title is similar to the search term
    public Page<TaskResponseDTO> searchTasksByFuzzyTitle(String searchTerm, Pageable pageable) {
        log.debug("searchTasksByFuzzyTitle called searchTerm='{}' pageable={}", searchTerm, pageable);
        Specification<Task> spec = Specification.where(TaskSpecifications.hasFuzzyTitle(searchTerm));
        Page<TaskResponseDTO> page = taskRepository.findAll(spec, pageable).map(taskMapper::toDTO);
        log.debug("searchTasksByFuzzyTitle returning {} results", page.getNumberOfElements());
        return page;
    }

    // CREATE — accepts TaskRequestDTO (no id, no completionStatus from client)
    @Transactional  // If anything fails, the whole operation is rolled back
    public TaskResponseDTO createTask(TaskRequestDTO requestDto) {
        // Step 1: Strip any HTML/JS from user input before doing anything else
        sanitizeRequest(requestDto);

        // Step 2: Business Validation (after sanitization, so we check the clean value)
        if (taskRepository.existsByHeaderAndCompletedFalse(requestDto.getTitle())) {
            log.warn("Duplicate task prevented for title={}", requestDto.getTitle());
            throw new DuplicateTaskException("You already have an active task with this title!");
        }
        Task taskEntity = taskMapper.toEntity(requestDto); // RequestDTO → Entity (id is null, DB assigns it)
        Task savedTask = taskRepository.save(taskEntity); // INSERT into DB
        TaskResponseDTO dto = taskMapper.toDTO(savedTask);                // Entity → ResponseDTO (with DB-generated id)
        log.debug("createTask saved id={}", dto.getId());
        return dto;
    }

    // UPDATE — accepts TaskRequestDTO (client cannot change the id via request body)
    @Transactional
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDto) {
        // Step 1: Strip any HTML/JS from user input before doing anything else
        sanitizeRequest(requestDto);

        // Step 2: Find the existing record (or throw 404)
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        // Step 3: Apply the sanitized values from the request onto the existing entity
        existingTask.setHeader(requestDto.getTitle());
        existingTask.setDescription(requestDto.getDescription());
        existingTask.setCompleted(requestDto.getCompleted());

        // Step 4: Save updated entity back to DB
        Task updatedTask = taskRepository.save(existingTask);

        // Step 5: Return as ResponseDTO
        TaskResponseDTO dto = taskMapper.toDTO(updatedTask);
        log.debug("updateTask saved id={}", dto.getId());
        return dto;
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    /**
     * Sanitizes all user-supplied string fields in the request DTO.
     * <p>
     * Why here and not in the DTO itself?
     * - DTOs are plain data holders; business logic belongs in the service layer.
     * - Keeping sanitization here means the DTO stays clean and testable.
     * - A single place to change if we ever switch sanitization libraries.
     * <p>
     * What does it do?
     * "<script>alert('x')</script>Buy groceries"  →  "Buy groceries"
     * "Buy groceries"                              →  "Buy groceries"  (unchanged)
     */
    private void sanitizeRequest(TaskRequestDTO requestDto) {
        requestDto.setTitle(sanitizationService.sanitize(requestDto.getTitle()));
        requestDto.setDescription(sanitizationService.sanitize(requestDto.getDescription()));
    }

    // DELETE
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        task.setDeleted(true);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
        log.debug("deleteTask marked deleted id={}", id);
    }
}
