package com.taskmanager.app.repository;

import com.taskmanager.app.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JpaRepository<EntityClass, PrimaryKeyDataType>
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Spring Boot generates all CRUD (Create, Read, Update, Delete) methods for you!
    // No code needed! Spring Data JPA generates everything.

    // Spring parses this name to create:
    // SELECT COUNT(*) > 0 FROM tasks WHERE title = ? AND completed = false
    boolean existsByHeaderAndCompletedFalse(String header);
}