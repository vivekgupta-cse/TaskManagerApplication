package com.taskmanager.app.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("Task with ID " + id + " not found");
    }

    public TaskNotFoundException(String header) {
        super("Task with Title " + header + " not found");
    }
}

