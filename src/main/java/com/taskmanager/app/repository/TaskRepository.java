package com.taskmanager.app.repository;

import com.taskmanager.app.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// JpaRepository<EntityClass, PrimaryKeyDataType>
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Spring Boot generates all CRUD (Create, Read, Update, Delete) methods for you!


}
