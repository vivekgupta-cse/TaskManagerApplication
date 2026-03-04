package com.taskmanager.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;


@Entity                     // Hibernate: manage this class as a database table and creates table
@Table(name = "tasks")      // The table will be named "tasks"
//@Data                       // Lombok: generates getters, setters, toString, equals, hashCode
@SQLDelete(sql = "UPDATE tasks SET deleted = true WHERE id=?") // Overrides DELETE command
@SQLRestriction("deleted = false") // Automatically filters all SELECT queries
@Getter
@Setter // Better than @Data for JPA entities
@NoArgsConstructor          // Lombok: generates empty constructor (REQUIRED by JPA)
@AllArgsConstructor         // Lombok: generates constructor with all fields
public class Task {

    @Id                                                    // Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // Auto-increment: 1, 2, 3...
    private Long id;

    @Column(name = "title", nullable = false, unique = true)  // DB column named "title", cannot be NULL
    private String header;                     // Java field is named differently!

    @Column(name = "description", length = 500)
    private String description;                // Maps to column "description"

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean completed;                 // Cannot be NULL, defaults to false

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
