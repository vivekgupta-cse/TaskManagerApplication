# TaskManager Application — Design Document & Tutorial

> **File:** `docs/DESIGN_AND_TUTORIAL.md`
> **Last Updated:** February 24, 2026

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Architecture](#3-architecture)
4. [Project Structure](#4-project-structure)
5. [Layer-by-Layer Explanation](#5-layer-by-layer-explanation)
   - [Entry Point](#51-entry-point--taskmanagerapplicationjava)
   - [Model (Entity)](#52-model-entity--taskjava)
   - [DTO](#53-dto--taskresponsedtojava)
   - [Mapper](#54-mapper--taskmapperjava)
   - [Repository](#55-repository--taskrepositoryj)
   - [Service](#56-service--taskservicejava)
   - [Controller](#57-controller--taskcontrollerjava)
   - [Exception Handling](#58-exception-handling)
6. [Configuration — application.yaml](#6-configuration--applicationyaml)
7. [Build Configuration — build.gradle.kts](#7-build-configuration--buildgradlekts)
8. [Data Flow — Request Lifecycle](#8-data-flow--request-lifecycle)
9. [API Reference](#9-api-reference)
10. [Key Concepts Explained](#10-key-concepts-explained)
11. [How Everything Connects](#11-how-everything-connects)

---

## 1. Project Overview

The **TaskManager Application** is a production-style RESTful web service for managing tasks.
It demonstrates a real-world layered architecture with proper separation of concerns,
custom exception handling, object mapping, and structured logging.

The API is available at: `http://localhost:9090/api/tasks`

---

## 2. Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 25 | Programming language |
| **Spring Boot** | 4.0.3 | Web framework & auto-configuration |
| **Spring Data JPA** | (managed by Spring Boot) | Database access layer |
| **Hibernate** | (managed by Spring Boot) | ORM — translates Java objects ↔ SQL |
| **PostgreSQL** | Latest (via Docker) | Production database |
| **Lombok** | Latest | Eliminates boilerplate code at compile time |
| **MapStruct** | 1.6.0.Beta1 | Auto-generates Entity ↔ DTO converters |
| **Gradle** | Latest wrapper | Build tool |
| **Logback** | (managed by Spring Boot) | Structured logging with file rolling |

---

## 3. Architecture

The application follows the classic **Layered (N-Tier) Architecture** pattern.
Each layer has one responsibility and communicates only with the layer directly below it.

```
 ┌───────────────────────────────────────────────────────┐
 │               CLIENT  (curl / browser / Postman)      │
 └───────────────────────────┬───────────────────────────┘
                             │  HTTP Request (JSON)
                             ▼
 ┌───────────────────────────────────────────────────────┐
 │                   CONTROLLER LAYER                    │
 │               TaskController.java                     │
 │   - Receives HTTP requests                            │
 │   - Validates path variables / request body           │
 │   - Delegates to Service                              │
 │   - Returns HTTP responses                            │
 └───────────────────────────┬───────────────────────────┘
                             │  calls with DTO
                             ▼
 ┌───────────────────────────────────────────────────────┐
 │                    SERVICE LAYER                      │
 │                 TaskService.java                      │
 │   - Contains ALL business logic                       │
 │   - Converts DTO ↔ Entity using TaskMapper            │
 │   - Calls Repository for data access                  │
 │   - Throws custom exceptions (TaskNotFoundException)  │
 └───────────────────────────┬───────────────────────────┘
                             │  calls with Entity
                             ▼
 ┌───────────────────────────────────────────────────────┐
 │                  REPOSITORY LAYER                     │
 │               TaskRepository.java                     │
 │   - Interface only — no code written by you           │
 │   - Spring Data JPA auto-generates all SQL queries    │
 └───────────────────────────┬───────────────────────────┘
                             │  SQL (via Hibernate/JDBC)
                             ▼
 ┌───────────────────────────────────────────────────────┐
 │             DATABASE  (PostgreSQL via Docker)         │
 │                      tasks table                      │
 └───────────────────────────────────────────────────────┘

 ┌───────────────────────────────────────────────────────┐
 │              CROSS-CUTTING CONCERNS                   │
 │  GlobalExceptionHandler → intercepts ALL exceptions   │
 │  Logback                → writes logs to file + console│
 └───────────────────────────────────────────────────────┘
```

**The Golden Rule:** Each layer only talks to the layer directly below it.
The Controller **never** touches the Repository or the database directly.

---

## 4. Project Structure

```
TaskManagerApplication/
├── build.gradle.kts                          ← Build config: dependencies, plugins
├── docker-compose.yml                        ← Starts PostgreSQL in Docker
├── src/
│   ├── main/
│   │   ├── java/com/taskmanager/app/
│   │   │   ├── TaskManagerApplication.java   ← main() — application entry point
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   └── TaskController.java       ← HTTP layer: receives & returns JSON
│   │   │   │
│   │   │   ├── service/
│   │   │   │   └── TaskService.java          ← Business logic layer
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   └── TaskRepository.java       ← Database layer (auto-implemented)
│   │   │   │
│   │   │   ├── model/
│   │   │   │   └── Task.java                 ← JPA Entity (maps to 'tasks' DB table)
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── TaskRequestDTO.java        ← Input contract (what clients send: POST/PUT)
│   │   │   │   └── TaskResponseDTO.java       ← Output contract (what clients receive)
│   │   │   │
│   │   │   ├── mapper/
│   │   │   │   └── TaskMapper.java           ← MapStruct: Entity ↔ DTO conversion
│   │   │   │
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java  ← Catches ALL exceptions centrally
│   │   │       ├── TaskNotFoundException.java   ← Custom 404 exception
│   │   │       └── ErrorResponse.java           ← Structured error JSON shape
│   │   │
│   │   └── resources/
│   │       └── application.yaml              ← DB config, port, logging config
│   │
│   └── test/
│       └── java/com/taskmanager/app/
│           ├── TaskManagerApplicationTests.java  ← Spring context smoke test
│           ├── controller/                       ← (empty — tests not yet written)
│           ├── service/                          ← (empty — tests not yet written)
│           └── exception/                        ← (empty — tests not yet written)
│
└── logs/
    └── app.log                               ← Rolling log file output
```

---

## 5. Layer-by-Layer Explanation

### 5.1 Entry Point — `TaskManagerApplication.java`

```java
@SpringBootApplication
public class TaskManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskManagerApplication.class, args);
    }
}
```

`@SpringBootApplication` is a shortcut for three annotations:

| Annotation | What it does |
|---|---|
| `@Configuration` | Marks this as a Spring configuration class |
| `@EnableAutoConfiguration` | Spring auto-configures Tomcat, JPA, Jackson, etc. |
| `@ComponentScan` | Scans this package and all sub-packages for Spring beans |

When `main()` runs, Spring Boot:
1. Starts an embedded **Tomcat** web server on port **9090**
2. Connects to **PostgreSQL** using credentials in `application.yaml`
3. Scans all classes annotated with `@RestController`, `@Service`, `@Repository`, etc.
4. Wires them all together via **Dependency Injection**

---

### 5.2 Model (Entity) — `Task.java`

The `Task` class represents a **row in the database**. JPA/Hibernate reads this class
and creates (or updates) the `tasks` table automatically.

```java
@Entity                     // Hibernate: manage this class as a database table
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
```

**Important naming mismatch:**
The Java field is `header` but the database column is `title`.
This is handled by `@Column(name = "title")` and explicitly mapped in `TaskMapper`.

**Resulting database table:**

```
tasks table (PostgreSQL):
┌────┬─────────────────────────┬──────────────────────────┬───────────┐
│ id │          title          │       description        │ completed │
├────┼─────────────────────────┼──────────────────────────┼───────────┤
│  1 │ Learn Spring Boot       │ Mastering the basics     │   false   │
│  2 │ Write Unit Tests        │ 100% coverage goal       │   true    │
│  3 │ Deploy to Production    │ Use Docker               │   false   │
└────┴─────────────────────────┴──────────────────────────┴───────────┘
```

---

### 5.3 DTOs — Request and Response

The application uses **two separate DTOs** — one for input, one for output.
This is the correct production pattern.

#### `TaskRequestDTO.java` — what clients SEND (POST / PUT body)

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Completion status must be specified")
    private boolean completed;  // boolean primitive — Lombok generates isCompleted()
}
```

- **No `id` field** — the database always generates the id, clients never send it
- **No `completionStatus`** — this is server-computed, clients never send it
- **Has validation annotations** — `@NotBlank`, `@Size` enforce input rules

#### `TaskResponseDTO.java` — what clients RECEIVE (all responses)

```java
@Data
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private boolean completed;
    private String completionStatus; // computed by server: "DONE" or "PENDING"
}
```

**Why two separate DTOs?**

```
TaskRequestDTO (CLIENT → SERVER)    TaskResponseDTO (SERVER → CLIENT)
┌────────────────────────┐          ┌──────────────────────────────┐
│ title       @NotBlank  │          │ id           (DB generated)  │
│ description @Size      │          │ title                        │
│ completed              │          │ description                  │
│                        │          │ completed                    │
│ NO id                  │          │ completionStatus  (computed) │
│ NO completionStatus    │          │   "DONE" or "PENDING"        │
└────────────────────────┘          └──────────────────────────────┘
```

| Concern | `TaskRequestDTO` | `TaskResponseDTO` |
|---|---|---|
| Has `id`? | ❌ No — DB generates it | ✅ Yes |
| Has validation? | ✅ Yes — `@NotBlank`, `@Size`, `@NotNull` | ❌ No — output needs none |
| Has `completionStatus`? | ❌ No — server computes it | ✅ Yes |
| Direction | Client → Server | Server → Client |
| Mass-assignment safe? | ✅ Yes — no `id` to hijack | N/A |

---

### 5.4 Mapper — `TaskMapper.java`

The Mapper converts between `Task` (Entity), `TaskRequestDTO` (input), and `TaskResponseDTO` (output).
**MapStruct** generates the implementation at compile time — you write only the interface.

```java
@Mapper(componentModel = "spring")  // Makes this a Spring Bean (injectable)
public interface TaskMapper {

    // Entity → ResponseDTO  (used by: getAllTasks, getTaskById, after createTask, after updateTask)
    @Mapping(source = "header", target = "title")   // field rename: header → title
    @Mapping(
        target = "completionStatus",
        expression = "java(task.isCompleted() ? \"DONE\" : \"PENDING\")"  // server-computed
    )
    TaskResponseDTO toDTO(Task task);

    // RequestDTO → Entity  (used by: createTask, updateTask)
    @Mapping(source = "title", target = "header")   // field rename: title → header
    @Mapping(target = "id", ignore = true)           // id is DB-generated, never from client
    Task toEntity(TaskRequestDTO dto);
}
```

**The two mapping directions:**

```
TaskRequestDTO  ──toEntity()──►  Task (Entity)  ──toDTO()──►  TaskResponseDTO
  (client input)                  (database)                   (client output)
```

MapStruct generates the actual implementation at **compile time**.
You can see the generated code at:
```
build/generated/sources/annotationProcessor/java/main/
  com/taskmanager/app/mapper/TaskMapperImpl.java
```

---

### 5.5 Repository — `TaskRepository.java`

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // No code needed! Spring Data JPA generates everything.
}
```

By extending `JpaRepository<Task, Long>`, Spring automatically provides:

| Method | Generated SQL |
|---|---|
| `findAll()` | `SELECT * FROM tasks` |
| `findById(id)` | `SELECT * FROM tasks WHERE id = ?` |
| `save(task)` | `INSERT INTO tasks ...` or `UPDATE tasks ...` |
| `delete(task)` | `DELETE FROM tasks WHERE id = ?` |
| `deleteById(id)` | `DELETE FROM tasks WHERE id = ?` |
| `existsById(id)` | `SELECT COUNT(*) > 0 FROM tasks WHERE id = ?` |
| `count()` | `SELECT COUNT(*) FROM tasks` |

`JpaRepository<Task, Long>` — the two type parameters mean:
- `Task` — the entity this repository manages
- `Long` — the data type of the primary key (`id` field)

---

### 5.6 Service — `TaskService.java`

The Service layer is the heart of the application. **All business logic lives here.**
The Controller only calls the Service — it never directly accesses the database.

```java
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
        Task taskEntity = taskMapper.toEntity(requestDto); // RequestDTO → Entity (id is null, DB assigns it)
        Task savedTask  = taskRepository.save(taskEntity); // INSERT into DB
        return taskMapper.toDTO(savedTask);                // Entity → ResponseDTO (with DB-generated id)
    }

    // UPDATE — accepts TaskRequestDTO (client cannot change the id via request body)
    @Transactional
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDto) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));  // 404 if not found
        existingTask.setHeader(requestDto.getTitle());
        existingTask.setDescription(requestDto.getDescription());
        existingTask.setCompleted(requestDto.isCompleted());
        Task updatedTask = taskRepository.save(existingTask);       // UPDATE in DB
        return taskMapper.toDTO(updatedTask);
    }

    // DELETE
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));  // 404 if not found
        taskRepository.delete(task);                                // DELETE from DB
    }
}
```

**Why `@Transactional`?**
It wraps the operation in a database transaction. If an exception occurs mid-way
(e.g., DB connection drops), all changes are **rolled back** automatically.
Without it, you could end up with partial writes corrupting your data.

---

### 5.7 Controller — `TaskController.java`

The Controller is the **front door** of the application. It has exactly three jobs:
1. Receive the HTTP request
2. Call the Service
3. Return the HTTP response

```java
@RestController               // = @Controller + @ResponseBody (auto-convert return value to JSON)
@RequestMapping("/api/tasks") // All endpoints in this class are under /api/tasks
@RequiredArgsConstructor      // Lombok: constructor injection for 'taskService'
public class TaskController {

    private final TaskService taskService;

    // GET /api/tasks  → returns all tasks as JSON array
    @GetMapping
    public List<TaskResponseDTO> getAllTasks() {
        return taskService.getAllTasks();
    }

    // GET /api/tasks/5  → returns single task or 404
    @GetMapping("/{id}")
    public TaskResponseDTO getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    // POST /api/tasks → creates a new task, returns the saved task (with DB-assigned id)
    // @Valid triggers @NotBlank / @Size validation on TaskRequestDTO — returns 400 if invalid
    @PostMapping
    public TaskResponseDTO createTask(@Valid @RequestBody TaskRequestDTO requestDto) {
        return taskService.createTask(requestDto);
    }

    // PUT /api/tasks/5 → updates existing task
    // @Valid triggers validation on TaskRequestDTO fields
    @PutMapping("/{id}")
    public TaskResponseDTO updateTask(@PathVariable Long id,
                                      @Valid @RequestBody TaskRequestDTO requestDto) {
        return taskService.updateTask(id, requestDto);
    }

    // DELETE /api/tasks/5  → deletes a task, returns HTTP 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();  // 204 — success, nothing to return
    }
}
```

**Key annotations explained:**

| Annotation | Meaning |
|---|---|
| `@RestController` | All methods return data (JSON), not view names |
| `@RequestMapping` | Base URL prefix for the whole class |
| `@GetMapping` | Maps HTTP GET requests |
| `@PostMapping` | Maps HTTP POST requests |
| `@PutMapping` | Maps HTTP PUT requests |
| `@DeleteMapping` | Maps HTTP DELETE requests |
| `@PathVariable` | Extracts `{id}` from the URL path |
| `@RequestBody` | Deserializes the JSON request body into a Java object |

---

### 5.8 Exception Handling

#### `TaskNotFoundException.java`

```java
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("Task with ID " + id + " not found");
    }
}
```

A custom exception that carries a meaningful message.
It extends `RuntimeException` so it does **not** need to be declared with `throws`.

#### `ErrorResponse.java`

The structured JSON shape returned for every error:

```java
@Getter
public class ErrorResponse {
    private final int status;             // e.g. 404
    private final String error;           // e.g. "Not Found"
    private final String message;         // e.g. "Task with ID 99 not found"
    private final LocalDateTime timestamp; // e.g. "2026-02-24T10:30:00"

    public ErrorResponse(int status, String error, String message) {
        this.status    = status;
        this.error     = error;
        this.message   = message;
        this.timestamp = LocalDateTime.now();
    }
}
```

#### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice  // Intercepts exceptions thrown from ANY @RestController
public class GlobalExceptionHandler {

    // Handles TaskNotFoundException → HTTP 404
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(404, "Not Found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Handles @Valid failures (e.g. @NotBlank, @Size violated) → HTTP 400
    // Collects ALL field errors into one message string
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse error = new ErrorResponse(400, "Bad Request", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Handles invalid input → HTTP 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(400, "Bad Request", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Catch-all for any unexpected exception → HTTP 500
    // NOTE: Never expose ex.getMessage() here — it may leak internal details!
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
                "Something went wrong. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

**How it works — the exception flow:**

```
GET /api/tasks/999
       ↓
  TaskController.getTaskById(999)
       ↓
  taskService.getTaskById(999)
       ↓
  taskRepository.findById(999)  →  returns Optional.empty()
       ↓
  .orElseThrow(...)  →  throws TaskNotFoundException("Task with ID 999 not found")
       ↓
  Spring intercepts the exception
       ↓
  GlobalExceptionHandler.handleTaskNotFound() is called
       ↓
  Returns HTTP 404 with body:
  {
    "status": 404,
    "error": "Not Found",
    "message": "Task with ID 999 not found",
    "timestamp": "2026-02-24T10:30:00"
  }
```

---

## 6. Configuration — `application.yaml`

```yaml
spring:
  application:
    name: TaskManagerApplication

  datasource:
    url: jdbc:postgresql://localhost:5432/taskdb   # PostgreSQL running in Docker
    username: docker
    password: docker
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update      # Auto-creates/updates tables on startup (don't use in prod!)
    show-sql: false          # Disabled — SQL goes to log file instead (see logging below)
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 9090                 # App runs on http://localhost:9090

logging:
  file:
    name: logs/app.log                          # Rolling log file
  logback:
    rollingpolicy:
      max-file-size: 10MB                       # Rotate when file hits 10MB
      max-history: 7                            # Keep last 7 rotated files
      file-name-pattern: logs/app-%d{yyyy-MM-dd}.%i.log
  level:
    root: INFO                                  # Default level
    com.example.TaskManagerApplication: DEBUG   # Application-level DEBUG logging
    org.hibernate.SQL: DEBUG                    # Prints all SQL queries to log file
    org.hibernate.orm.jdbc.bind: TRACE          # Prints actual values bound to ? placeholders
```

**`ddl-auto` options explained:**

| Value | Behaviour |
|---|---|
| `create` | Drop and recreate schema on every startup (data loss!) |
| `create-drop` | Like `create`, but also drops schema on shutdown |
| `update` | Add missing tables/columns, but never drop anything |
| `validate` | Validate schema matches entities, throw error if mismatch |
| `none` | Do nothing — manage schema yourself (production best practice) |

---

## 7. Build Configuration — `build.gradle.kts`

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter")             // Core Spring Boot auto-configuration
    implementation("org.springframework.boot:spring-boot-starter-web")        // REST API
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")   // JPA + Hibernate
    implementation("org.springframework.boot:spring-boot-starter-validation") // @Valid, @NotBlank, @Size
    runtimeOnly("org.postgresql:postgresql")                                   // PostgreSQL driver

    compileOnly("org.projectlombok:lombok")                // Lombok available at compile time only
    annotationProcessor("org.projectlombok:lombok")        // Runs Lombok's code generator

    implementation("org.mapstruct:mapstruct:1.6.0.Beta1")              // MapStruct runtime
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.0.Beta1") // MapStruct code generator
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0") // Ensures Lombok runs before MapStruct

    testImplementation("org.springframework.boot:spring-boot-starter-test") // JUnit, Mockito, MockMvc
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")           // JUnit platform launcher (runtime only)
}
```

**Dependency scope explained:**

| Scope | Meaning |
|---|---|
| `implementation` | Available at compile time AND runtime — packaged into the final JAR |
| `compileOnly` | Available at compile time ONLY — NOT in the final JAR (Lombok's job is done after compilation) |
| `runtimeOnly` | NOT available at compile time — only at runtime (the DB driver only runs when the app runs) |
| `annotationProcessor` | Run during compilation to generate source code (Lombok, MapStruct) |
| `testImplementation` | Available for test code only — not in production JAR |

---

## 8. Data Flow — Request Lifecycle

### GET /api/tasks/{id} — Fetch a single task

```
Client: GET /api/tasks/3
  │
  ▼
[TaskController]
  getTaskById(3)
  │
  ▼
[TaskService]
  taskRepository.findById(3)
  │
  ├─── Found ──────────────────────────────────────────────────────┐
  │                                                                │
  │  [TaskRepository]                                             │
  │    SELECT * FROM tasks WHERE id = 3                           │
  │    Returns: Task{id=3, header="Deploy", completed=false}      │
  │                                                               │
  │  [TaskMapper]                                                 │
  │    task.header      → dto.title = "Deploy"                    │
  │    task.completed   → dto.completionStatus = "PENDING"        │
  │    Returns: TaskResponseDTO                                   │
  │                                                               │
  │  [TaskController]                                             │
  │    Spring serializes DTO → JSON                               │
  │                                                               │
  └─► Client receives: HTTP 200                                   │
      { "id": 3, "title": "Deploy", "completed": false,          │
        "completionStatus": "PENDING" }                           │
  │
  └─── Not Found ──────────────────────────────────────────────────┐
       [TaskService] throws TaskNotFoundException(3)               │
       [GlobalExceptionHandler] catches it                         │
       Client receives: HTTP 404                                   │
       { "status": 404, "error": "Not Found",                     │
         "message": "Task with ID 3 not found",                   │
         "timestamp": "2026-02-24T10:30:00" }                     │
```

### POST /api/tasks — Create a new task

```
Client: POST /api/tasks
        Body: { "title": "Learn Spring Boot", "description": "Basics", "completed": false }
  │
  ▼
[Spring Boot]
  Deserializes JSON body → TaskRequestDTO object
  │
  ▼
[TaskController]
  createTask(dto)  — @Valid triggers validation on TaskRequestDTO fields
  │
  ▼
[TaskService]
  taskMapper.toEntity(dto)
    → Task{id=null, header="Learn Spring Boot", description="Basics", completed=false}
  │
  ▼
[TaskRepository]
  taskRepository.save(task)
    → INSERT INTO tasks (title, description, completed) VALUES (?, ?, ?)
    → Database assigns id = 4
    → Returns: Task{id=4, header="Learn Spring Boot", ...}
  │
  ▼
[TaskService]
  taskMapper.toDTO(savedTask)
    → TaskResponseDTO{id=4, title="Learn Spring Boot", completionStatus="PENDING", ...}
  │
  ▼
[Spring Boot]
  Serializes DTO → JSON
  │
  ▼
Client receives: HTTP 200
  { "id": 4, "title": "Learn Spring Boot", "description": "Basics",
    "completed": false, "completionStatus": "PENDING" }
```

---

## 9. API Reference

### Base URL: `http://localhost:9090/api/tasks`

| Method | Endpoint | Request Body | Success Response | Error Response |
|---|---|---|---|---|
| GET | `/api/tasks` | None | 200 + Array of tasks | — |
| GET | `/api/tasks/{id}` | None | 200 + Single task | 404 if not found |
| POST | `/api/tasks` | Task JSON | 200 + Created task (with id) | 500 on error |
| PUT | `/api/tasks/{id}` | Task JSON | 200 + Updated task | 404 if not found |
| DELETE | `/api/tasks/{id}` | None | 204 No Content | 404 if not found |

### Request Body Shape (for POST and PUT) — `TaskRequestDTO`

```json
{
  "title": "Learn Spring Boot",
  "description": "Mastering the basics",
  "completed": false
}
```

> **Note:** Do NOT send `id` or `completionStatus` — they are ignored on input.
> `title` is required (`@NotBlank`). Sending an empty title returns HTTP 400.

### Validation Error Response (HTTP 400) — when `@NotBlank` / `@Size` violated

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "title: Title is required",
  "timestamp": "2026-02-24T10:30:00"
}
```

### Response Shape (all GET / POST / PUT responses) — `TaskResponseDTO`

```json
{
  "id": 1,
  "title": "Learn Spring Boot",
  "description": "Mastering the basics",
  "completed": false,
  "completionStatus": "PENDING"
}
```

### Error Response Shape (all errors)

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Task with ID 99 not found",
  "timestamp": "2026-02-24T10:30:00"
}
```

### curl Examples

```bash
# 1. Get all tasks
curl http://localhost:9090/api/tasks

# 2. Get task by ID
curl http://localhost:9090/api/tasks/1

# 3. Create a new task
curl -X POST http://localhost:9090/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Spring Boot","description":"Mastering the basics","completed":false}'

# 4. Update a task
curl -X PUT http://localhost:9090/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Spring Boot","description":"Advanced topics","completed":true}'

# 5. Delete a task
curl -X DELETE http://localhost:9090/api/tasks/1
```

---

## 10. Key Concepts Explained

### Dependency Injection (DI)

Spring creates and manages all objects (called **beans**). You never call `new` for services.

```java
// BAD — tightly coupled, impossible to unit test
public class TaskController {
    private TaskService taskService = new TaskService(new TaskRepository(...));
}

// GOOD — Spring injects it, easy to mock in tests
public class TaskController {
    private final TaskService taskService;  // Spring provides this

    // @RequiredArgsConstructor generates this constructor:
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }
}
```

### Optional\<T\> — Safely handling "might not exist"

```java
// findById() returns Optional<Task> — it either has a value or is empty
Optional<Task> result = taskRepository.findById(id);

// BAD — crashes with NoSuchElementException if empty
Task task = result.get();

// GOOD — throws your custom exception, handled by GlobalExceptionHandler → 404
Task task = result.orElseThrow(() -> new TaskNotFoundException(id));
```

### @Transactional

```java
@Transactional
public TaskResponseDTO createTask(TaskResponseDTO dto) {
    Task entity = taskMapper.toEntity(dto);
    // If taskRepository.save() throws an exception here,
    // everything is rolled back — no partial data in DB
    Task saved = taskRepository.save(entity);
    return taskMapper.toDTO(saved);
}
```

### Lombok Annotations Quick Reference

| Annotation | Generated code |
|---|---|
| `@Getter` | `getField()` for every field |
| `@Setter` | `setField(value)` for every field |
| `@NoArgsConstructor` | `public Task() {}` |
| `@AllArgsConstructor` | `public Task(Long id, String header, ...)` |
| `@Data` | `@Getter + @Setter + @ToString + @EqualsAndHashCode` |
| `@RequiredArgsConstructor` | Constructor for all `final` fields only |

### Spring Annotations Quick Reference

| Annotation | Meaning |
|---|---|
| `@SpringBootApplication` | Entry point — enables everything |
| `@RestController` | HTTP controller that returns JSON |
| `@RequestMapping` | Base URL path for the class |
| `@GetMapping` | Handle HTTP GET |
| `@PostMapping` | Handle HTTP POST |
| `@PutMapping` | Handle HTTP PUT |
| `@DeleteMapping` | Handle HTTP DELETE |
| `@PathVariable` | Extract `{id}` from URL |
| `@RequestBody` | Parse JSON body into Java object |
| `@Service` | Business logic bean |
| `@Repository` | Data access bean |
| `@Entity` | JPA-managed database table |
| `@RestControllerAdvice` | Global exception interceptor |
| `@ExceptionHandler` | Method handles a specific exception type |
| `@Transactional` | Wrap method in a DB transaction |

---

## 11. How Everything Connects

```
                        TaskManagerApplication.java
                                   │
                     @SpringBootApplication scans everything
                                   │
           ┌───────────────────────┼───────────────────────┐
           │                       │                       │
           ▼                       ▼                       ▼
  TaskController.java      GlobalException           application.yaml
  (@RestController)         Handler.java             (config & DB)
           │                (@RestControllerAdvice)
           │ calls                 │ intercepts
           ▼                 exceptions from
  TaskService.java           TaskController
  (@Service)
           │ uses
     ┌─────┴─────┐
     ▼           ▼
TaskRepository  TaskMapper
(@Repository)  (@Mapper)
     │           │
     │ SQL     converts
     ▼           ▼
PostgreSQL    Task  ↔  TaskResponseDTO
 Database
(tasks table)
```

**Data types that flow between layers:**

```
HTTP JSON (POST body)  →  [Controller]  →  TaskRequestDTO  (@Valid enforced)
TaskRequestDTO         →  [Service]     →  Task (Entity)   (via TaskMapper.toEntity)
Task (Entity)          →  [Repository]  →  SQL / PostgreSQL
PostgreSQL             →  [Repository]  →  Task (Entity)
Task (Entity)          →  [Service]     →  TaskResponseDTO  (via TaskMapper.toDTO)
TaskResponseDTO        →  [Controller]  →  HTTP JSON (response)
```

### The Three Rules of this Architecture

1. **Controllers** accept and return DTOs — they never touch Entities or the database
2. **Services** contain all logic — they convert between DTOs and Entities using the Mapper
3. **Repositories** only deal with Entities — they know nothing about DTOs or HTTP

Following these rules makes every layer independently:
- **Testable** — mock the layer below, test the layer in isolation
- **Changeable** — swap PostgreSQL for MongoDB without touching the Controller
- **Understandable** — each file has one clear job

