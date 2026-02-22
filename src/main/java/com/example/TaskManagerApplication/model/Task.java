package com.example.TaskManagerApplication.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // Tells Hibernate to create a table for this class
@Table(name = "tasks")
@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id // Marks this as the Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment (1, 2, 3...)
    private Long id;

    @Column(nullable = false) // Database-level constraint
    private String title;

    private String description;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean completed;
}
