package com.taskmanager.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity                     // Hibernate: manage this class as a database table and creates table
@Table(name = "tasks")      // The table will be named "tasks"
@Data                       // Lombok: generates getters, setters, toString, equals, hashCode
@NoArgsConstructor          // Lombok: generates empty constructor (REQUIRED by JPA)
@AllArgsConstructor         // Lombok: generates constructor with all fields
public class Task {

    @Id                                                    // Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // Auto-increment: 1, 2, 3...
    private Long id;

    @Column(nullable = false, name = "title")  // DB column named "title", cannot be NULL
    private String header;                     // Java field is named differently!

    private String description;                // Maps to column "description"

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean completed;                 // Cannot be NULL, defaults to false
}
