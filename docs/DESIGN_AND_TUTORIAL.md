# TaskManager Application — Design Document & Tutorial

> **Last Updated:** February 24, 2026

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Architecture](#3-architecture)
4. [Project Structure](#4-project-structure)
5. [Layer-by-Layer Explanation](#5-layer-by-layer-explanation)
   - [5.1 Entry Point](#51-entry-point)
   - [5.2 Model — Entity](#52-model--entity)
   - [5.3 DTOs — Request and Response](#53-dtos--request-and-response)
   - [5.4 Mapper](#54-mapper)
   - [5.5 Repository](#55-repository)
   - [5.6 Service](#56-service)
   - [5.7 Controller](#57-controller)
   - [5.8 Exception Handling](#58-exception-handling)
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

**API base URL:** `http://localhost:9090/api/tasks`

---

## 2. Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 25 | Programming language |
| Spring Boot | 4.0.3 | Web framework and auto-configuration |
| Spring Data JPA | (managed by Spring Boot) | Database access layer |
| Hibernate | (managed by Spring Boot) | ORM — translates Java objects to/from SQL |
| PostgreSQL | Latest (via Docker) | Production database |
| Lombok | Latest | Eliminates boilerplate code at compile time |
| MapStruct | 1.6.0.Beta1 | Auto-generates Entity ↔ DTO converters at compile time |
| Gradle | 9.3.0 (wrapper) | Build tool |
| Logback | (managed by Spring Boot) | Structured logging with rolling file support |

---

## 3. Architecture

The application follows the classic **Layered (N-Tier) Architecture** pattern.
Each layer has one responsibility and communicates only with the layer directly below it.

```
+----------------------------------------------------------+
|            CLIENT  (curl / browser / Postman)            |
+----------------------------+-----------------------------+
                             |  HTTP Request (JSON)
                             v
+----------------------------------------------------------+
|                    CONTROLLER LAYER                      |
|                  TaskController.java                     |
|  - Receives HTTP requests                                |
|  - Validates request body (@Valid)                       |
|  - Delegates to Service layer                            |
|  - Returns HTTP responses                                |
+----------------------------+-----------------------------+
                             |  TaskRequestDTO / id
                             v
+----------------------------------------------------------+
|                     SERVICE LAYER                        |
|                   TaskService.java                       |
|  - Contains ALL business logic                           |
|  - Converts DTO <-> Entity using TaskMapper              |
|  - Calls Repository for data access                      |
|  - Throws custom exceptions (TaskNotFoundException)      |
+----------------------------+-----------------------------+
                             |  Task (Entity)
                             v
+----------------------------------------------------------+
|                   REPOSITORY LAYER                       |
|                 TaskRepository.java                      |
|  - Interface only — no code written by you               |
|  - Spring Data JPA auto-generates all SQL queries        |
+----------------------------+-----------------------------+
                             |  SQL via Hibernate / JDBC
                             v
+----------------------------------------------------------+
|           DATABASE  (PostgreSQL running in Docker)       |
|                        tasks table                       |
+----------------------------------------------------------+

+----------------------------------------------------------+
|                 CROSS-CUTTING CONCERNS                   |
|  GlobalExceptionHandler — intercepts ALL exceptions      |
|  Logback                — writes structured logs to file |
+----------------------------------------------------------+
```

**The Golden Rule:** Each layer only talks to the layer directly below it.
The Controller **never** touches the Repository or the database directly.

---

## 4. Project Structure

```
TaskManagerApplication/
+-- build.gradle.kts                           <- Build config: dependencies, plugins
+-- docker-compose.yml                         <- Starts PostgreSQL in Docker
+-- src/
|   +-- main/
|   |   +-- java/com/taskmanager/app/
|   |   |   +-- TaskManagerApplication.java       <- main() — application entry point
|   |   |   +-- controller/
|   |   |   |   +-- TaskController.java           <- HTTP layer: receives and returns JSON
|   |   |   +-- service/
|   |   |   |   +-- TaskService.java              <- Business logic layer
|   |   |   +-- repository/
|   |   |   |   +-- TaskRepository.java           <- DB layer (auto-implemented by Spring)
|   |   |   +-- model/
|   |   |   |   +-- Task.java                     <- JPA Entity — maps to 'tasks' DB table
|   |   |   +-- dto/
|   |   |   |   +-- TaskRequestDTO.java           <- Input contract (POST/PUT request body)
|   |   |   |   +-- TaskResponseDTO.java          <- Output contract (all responses)
|   |   |   +-- mapper/
|   |   |   |   +-- TaskMapper.java               <- MapStruct: Entity <-> DTO conversion
|   |   |   +-- exception/
|   |   |       +-- GlobalExceptionHandler.java   <- Catches ALL exceptions centrally
|   |   |       +-- TaskNotFoundException.java    <- Custom 404 exception
|   |   |       +-- ErrorResponse.java            <- Structured error JSON shape
|   |   +-- resources/
|   |       +-- application.yaml                  <- DB config, port, logging
|   +-- test/
|       +-- java/com/taskmanager/app/
|           +-- TaskManagerApplicationTests.java   <- Spring context smoke test
|           +-- controller/                        <- (empty — tests not yet written)
|           +-- service/                           <- (empty — tests not yet written)
|           +-- exception/                         <- (empty — tests not yet written)
+-- logs/
|   +-- app.log                                <- Rolling log file output
+-- build/
    +-- generated/sources/annotationProcessor/
        +-- ...TaskMapperImpl.java             <- MapStruct-generated implementation
```

---

## 5. Layer-by-Layer Explanation

### 5.1 Entry Point

**`TaskManagerApplication.java`**

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
2. Connects to **PostgreSQL** using credentials from `application.yaml`
3. Scans all classes annotated with `@RestController`, `@Service`, `@Repository`, etc.
4. Wires them all together via **Dependency Injection**

---

### 5.2 Model — Entity

**`Task.java`**

```java
@Entity                     // Hibernate: manage this class as a database table
@Table(name = "tasks")      // The table will be named "tasks"
@Data                       // Lombok: getters, setters, toString, equals, hashCode
@NoArgsConstructor          // Lombok: empty constructor — REQUIRED by JPA
@AllArgsConstructor         // Lombok: constructor with all fields
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment: 1, 2, 3...
    private Long id;

    @Column(nullable = false, name = "title")  // DB column is "title", cannot be NULL
    private String header;                     // Java field is "header" — intentionally different!

    private String description;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean completed;                 // primitive boolean — cannot be NULL
}
```

**Key detail — intentional naming mismatch:**

| Location | Name used |
|---|---|
| Java field | `header` |
| Database column | `title` (via `@Column(name = "title")`) |
| Client JSON (in and out) | `title` |

This mismatch is bridged by `TaskMapper` using `@Mapping(source = "header", target = "title")`.

**Resulting database table:**

```
tasks table (PostgreSQL):
+----+----------------------+------------------------+-----------+
| id |        title         |      description       | completed |
+----+----------------------+------------------------+-----------+
|  1 | Learn Spring Boot    | Mastering the basics   |   false   |
|  2 | Write Unit Tests     | 100% coverage goal     |   true    |
|  3 | Deploy to Production | Use Docker             |   false   |
+----+----------------------+------------------------+-----------+
```

---

### 5.3 DTOs — Request and Response

The application uses **two separate DTOs** — one for input, one for output.
This is the correct production pattern: **never expose your Entity directly over HTTP.**

#### `TaskRequestDTO.java` — what clients SEND (POST / PUT body)

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

- **No `id` field** — the database always generates it; clients must never send it
- **No `completionStatus`** — this is server-computed, clients never send it
- **Validation annotations** — `@NotBlank`, `@Size`, `@NotNull` enforce input rules
- **`boolean` primitive** (not `Boolean` wrapper) — Lombok generates `isCompleted()`, not `getCompleted()`

#### `TaskResponseDTO.java` — what clients RECEIVE (all responses)

```java
@Data
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private boolean completed;
    private String completionStatus; // server-computed: "DONE" or "PENDING" — NOT in the DB
}
```

**Why two separate DTOs?**

```
TaskRequestDTO (CLIENT -> SERVER)       TaskResponseDTO (SERVER -> CLIENT)
+------------------------------+        +-----------------------------------+
| title       @NotBlank        |        | id            (DB generated)      |
|             @Size(min=3,     |        | title                             |
|                   max=100)   |        | description                       |
| description @Size(max=500)   |        | completed                         |
| completed   @NotNull         |        | completionStatus  "DONE"/"PENDING" |
|                              |        |                   (server-computed)|
| NO id                        |        |                                   |
| NO completionStatus          |        |                                   |
+------------------------------+        +-----------------------------------+
```

| Concern | `TaskRequestDTO` | `TaskResponseDTO` |
|---|---|---|
| Has `id`? | No — DB generates it | Yes |
| Has validation? | Yes — `@NotBlank`, `@Size`, `@NotNull` | No — output needs none |
| Has `completionStatus`? | No — server computes it | Yes |
| Direction | Client to Server | Server to Client |
| Mass-assignment safe? | Yes — client can never set the `id` | N/A |

---

### 5.4 Mapper

**`TaskMapper.java`**

```java
@Mapper(componentModel = "spring")  // Makes this a Spring Bean — injectable via constructor
public interface TaskMapper {

    // Entity -> ResponseDTO  (used after every read and write)
    @Mapping(source = "header", target = "title")  // header (Java) -> title (JSON)
    @Mapping(
        target = "completionStatus",
        expression = "java(task.isCompleted() ? \"DONE\" : \"PENDING\")"
    )
    TaskResponseDTO toDTO(Task task);

    // RequestDTO -> Entity  (used by createTask and updateTask)
    @Mapping(source = "title", target = "header")  // title (JSON) -> header (Java)
    @Mapping(target = "id", ignore = true)          // id is always DB-generated — never from client
    Task toEntity(TaskRequestDTO dto);
}
```

MapStruct reads this interface at **compile time** and generates `TaskMapperImpl.java` automatically.
You write only the interface — MapStruct writes all the boring conversion code for you.

**The two mapping directions:**

```
TaskRequestDTO --toEntity()--> Task (Entity) --toDTO()--> TaskResponseDTO
 (client input)                 (database row)              (client output)
```

The generated implementation can be found at:
```
build/generated/sources/annotationProcessor/java/main/
    com/taskmanager/app/mapper/TaskMapperImpl.java
```

---

### 5.5 Repository

**`TaskRepository.java`**

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // No code needed — Spring Data JPA generates all SQL operations automatically!
}
```

By extending `JpaRepository<Task, Long>`, Spring automatically provides:

| Method | Generated SQL |
|---|---|
| `findAll()` | `SELECT * FROM tasks` |
| `findById(id)` | `SELECT * FROM tasks WHERE id = ?` |
| `save(task)` | `INSERT INTO tasks ...` or `UPDATE tasks SET ... WHERE id = ?` |
| `delete(task)` | `DELETE FROM tasks WHERE id = ?` |
| `existsById(id)` | `SELECT COUNT(*) > 0 FROM tasks WHERE id = ?` |
| `count()` | `SELECT COUNT(*) FROM tasks` |

The two type parameters `JpaRepository<Task, Long>` mean:
- `Task` — the Entity this repository manages
- `Long` — the data type of the primary key

`findById()` returns `Optional<Task>` — not `Task` directly.
This forces you to explicitly handle the "not found" case (see Service below).

---

### 5.6 Service

**`TaskService.java`**

```java
@Service                   // Marks this as a Spring-managed service bean
@RequiredArgsConstructor   // Lombok: generates constructor for all 'final' fields
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    // READ ALL
    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAll()    // SELECT * FROM tasks -> List<Task>
                .stream()
                .map(taskMapper::toDTO)    // each Task -> TaskResponseDTO
                .toList();                 // collect into new list (safe defensive copy)
    }

    // READ ONE
    public TaskResponseDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));  // throws 404 if not found
        return taskMapper.toDTO(task);
    }

    // CREATE
    @Transactional
    public TaskResponseDTO createTask(TaskRequestDTO requestDto) {
        Task taskEntity = taskMapper.toEntity(requestDto); // DTO -> Entity (id is null here)
        Task savedTask  = taskRepository.save(taskEntity); // INSERT — DB assigns the id
        return taskMapper.toDTO(savedTask);                // Entity -> ResponseDTO (with real id)
    }

    // UPDATE
    @Transactional
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDto) {
        Task existingTask = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        existingTask.setHeader(requestDto.getTitle());
        existingTask.setDescription(requestDto.getDescription());
        existingTask.setCompleted(requestDto.isCompleted()); // boolean primitive -> isCompleted()

        Task updatedTask = taskRepository.save(existingTask); // UPDATE in DB
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
```

**Why `@Transactional`?**
It wraps the entire method in a database transaction. If an exception occurs mid-way
(e.g., DB connection drops after a partial write), all changes are **rolled back** automatically.
Without it, you could end up with corrupt partial writes in the database.

---

### 5.7 Controller

**`TaskController.java`**

```java
@RestController               // = @Controller + @ResponseBody — auto-converts returns to JSON
@RequestMapping("/api/tasks") // All endpoints in this class are prefixed with /api/tasks
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public List<TaskResponseDTO> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")
    public TaskResponseDTO getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    // @Valid triggers @NotBlank / @Size / @NotNull on TaskRequestDTO — 400 Bad Request if invalid
    @PostMapping
    public TaskResponseDTO createTask(@Valid @RequestBody TaskRequestDTO requestDto) {
        return taskService.createTask(requestDto);
    }

    @PutMapping("/{id}")
    public TaskResponseDTO updateTask(@PathVariable Long id,
                                      @Valid @RequestBody TaskRequestDTO requestDto) {
        return taskService.updateTask(id, requestDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();  // HTTP 204 — success, no response body
    }
}
```

**Key annotations:**

| Annotation | Meaning |
|---|---|
| `@RestController` | All methods return data (JSON), not HTML view names |
| `@RequestMapping` | Base URL prefix for the whole class |
| `@GetMapping` | Maps HTTP GET requests |
| `@PostMapping` | Maps HTTP POST requests |
| `@PutMapping` | Maps HTTP PUT requests |
| `@DeleteMapping` | Maps HTTP DELETE requests |
| `@PathVariable` | Extracts `{id}` from the URL path |
| `@RequestBody` | Deserializes the JSON request body into a Java object |
| `@Valid` | Triggers Bean Validation on the annotated parameter |

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

Extends `RuntimeException` so it does **not** need to be declared with `throws` on method signatures.

---

#### `ErrorResponse.java`

The structured JSON shape returned for every error:

```java
@Getter  // Jackson needs getters to serialize fields to JSON
public class ErrorResponse {
    private final int status;              // HTTP status code, e.g. 404
    private final String error;            // HTTP status phrase, e.g. "Not Found"
    private final String message;          // Human-readable description of the problem
    private final LocalDateTime timestamp; // When the error occurred — auto-set in constructor

    public ErrorResponse(int status, String error, String message) {
        this.status    = status;
        this.error     = error;
        this.message   = message;
        this.timestamp = LocalDateTime.now();
    }
}
```

---

#### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice  // Intercepts exceptions thrown from ANY @RestController globally
public class GlobalExceptionHandler {

    // TaskNotFoundException -> HTTP 404
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),           // 404
                HttpStatus.NOT_FOUND.getReasonPhrase(), // "Not Found"
                ex.getMessage()                         // "Task with ID 99 not found"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // @Valid failures (@NotBlank, @Size, @NotNull violated) -> HTTP 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // IllegalArgumentException -> HTTP 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Catch-all -> HTTP 500
    // NEVER expose ex.getMessage() here — it may leak internal implementation details!
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Something went wrong. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

**How the exception flow works — example:**

```
GET /api/tasks/999
       |
       v
  TaskController.getTaskById(999)
       |
       v
  taskService.getTaskById(999)
       |
       v
  taskRepository.findById(999)  ->  Optional.empty()
       |
       v
  .orElseThrow()  ->  throws TaskNotFoundException("Task with ID 999 not found")
       |
       v
  Spring intercepts — bypasses normal response flow
       |
       v
  GlobalExceptionHandler.handleTaskNotFound() is called
       |
       v
  HTTP 404 response:
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
    url: jdbc:postgresql://localhost:5432/taskdb
    username: docker
    password: docker
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update      # Auto-creates/updates tables on startup — never use in production!
    show-sql: false          # Disabled — SQL goes to log file instead (see logging below)
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 9090

logging:
  file:
    name: logs/app.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 7
      file-name-pattern: logs/app-%d{yyyy-MM-dd}.%i.log
  level:
    root: INFO
    com.example.TaskManagerApplication: DEBUG     # Application code at DEBUG level
    org.hibernate.SQL: DEBUG                      # SQL queries go to log file
    org.hibernate.orm.jdbc.bind: TRACE            # Actual values bound to ? placeholders
```

**`ddl-auto` options:**

| Value | Behaviour |
|---|---|
| `create` | Drop and recreate schema on every startup — data loss! |
| `create-drop` | Like `create`, but also drops schema on shutdown |
| `update` | Add missing columns/tables, never drop anything — safe for dev |
| `validate` | Validate schema matches entities, throw error if mismatch |
| `none` | Do nothing — manage schema yourself — production best practice |

---

## 7. Build Configuration — `build.gradle.kts`

```kotlin
plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")              // Core Spring Boot auto-configuration
    implementation("org.springframework.boot:spring-boot-starter-web")         // REST API (Tomcat + Jackson)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")    // JPA + Hibernate
    implementation("org.springframework.boot:spring-boot-starter-validation")  // @Valid, @NotBlank, @Size, @NotNull
    runtimeOnly("org.postgresql:postgresql")                                    // PostgreSQL JDBC driver

    compileOnly("org.projectlombok:lombok")                                     // Lombok — compile time only
    annotationProcessor("org.projectlombok:lombok")                             // Runs Lombok's code generator

    implementation("org.mapstruct:mapstruct:1.6.0.Beta1")                       // MapStruct runtime
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.0.Beta1")        // MapStruct code generator
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")     // Ensures Lombok runs BEFORE MapStruct

    testImplementation("org.springframework.boot:spring-boot-starter-test")     // JUnit 5, Mockito, MockMvc
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")               // JUnit platform launcher
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

**Dependency scope explained:**

| Scope | Available when | In JAR? | Use case |
|---|---|---|---|
| `implementation` | Compile + runtime | Yes | Regular dependencies (web, jpa, mapstruct) |
| `compileOnly` | Compile only | No | Lombok — job is done after compilation |
| `runtimeOnly` | Runtime only | Yes | PostgreSQL driver — only needed at runtime |
| `annotationProcessor` | During compilation only | No | Code generators: Lombok, MapStruct |
| `testImplementation` | Test compile + runtime | No | JUnit, Mockito |
| `testRuntimeOnly` | Test runtime only | No | JUnit platform launcher |

---

## 8. Data Flow — Request Lifecycle

### GET /api/tasks/{id} — Fetch a single task

```
Client: GET /api/tasks/3
  |
  v
[TaskController] getTaskById(3)
  |
  v
[TaskService] taskRepository.findById(3)
  |
  +-- Found -----------------------------------------------------------------+
  |   [TaskRepository] SELECT * FROM tasks WHERE id = 3                     |
  |   Returns: Task { id=3, header="Deploy to Prod", completed=false }      |
  |   [TaskMapper.toDTO()]                                                   |
  |     header    -> title = "Deploy to Prod"                               |
  |     completed -> completionStatus = "PENDING"                           |
  |   [Jackson serializes TaskResponseDTO to JSON]                          |
  +-> HTTP 200: { "id":3, "title":"Deploy to Prod", "completionStatus":"PENDING" ... }
  |
  +-- Not Found -------------------------------------------------------------+
      throws TaskNotFoundException(3)
      GlobalExceptionHandler.handleTaskNotFound() catches it
      HTTP 404: { "status":404, "error":"Not Found", "message":"Task with ID 3 not found" }
```

### POST /api/tasks — Create a new task

```
Client: POST /api/tasks
        Body: { "title": "Learn Spring Boot", "description": "Basics", "completed": false }
  |
  v
[Jackson] Deserializes JSON -> TaskRequestDTO object
  |
  v
[TaskController] @Valid triggers Bean Validation on TaskRequestDTO
  If invalid -> MethodArgumentNotValidException -> GlobalExceptionHandler -> HTTP 400
  If valid   -> taskService.createTask(requestDto)
  |
  v
[TaskService]
  taskMapper.toEntity(requestDto)
    -> Task { id=null, header="Learn Spring Boot", description="Basics", completed=false }
  taskRepository.save(task)
    -> INSERT INTO tasks (title, description, completed) VALUES (?, ?, ?)
    -> DB auto-assigns id = 4
    -> Returns: Task { id=4, header="Learn Spring Boot", ... }
  taskMapper.toDTO(savedTask)
    -> TaskResponseDTO { id=4, title="Learn Spring Boot", completionStatus="PENDING", ... }
  |
  v
HTTP 200: { "id":4, "title":"Learn Spring Boot", "completed":false, "completionStatus":"PENDING" }
```

### PUT /api/tasks/{id} — Update an existing task

```
Client: PUT /api/tasks/4
        Body: { "title": "Learn Spring Boot", "description": "Advanced", "completed": true }
  |
  v
[TaskController] @Valid -> [TaskService]
  1. taskRepository.findById(4)       -> load existing Task (or throw 404)
  2. existingTask.setHeader(...)      -> apply new values from RequestDTO
  3. existingTask.setDescription(...) 
  4. existingTask.setCompleted(...)
  5. taskRepository.save(existing)    -> UPDATE tasks SET ... WHERE id = 4
  6. taskMapper.toDTO(updated)        -> TaskResponseDTO
  |
  v
HTTP 200: updated task as JSON
```

### DELETE /api/tasks/{id}

```
Client: DELETE /api/tasks/4
  |
  v
[TaskController] -> [TaskService]
  1. taskRepository.findById(4)   -> load Task (or throw 404)
  2. taskRepository.delete(task)  -> DELETE FROM tasks WHERE id = 4
  |
  v
HTTP 204 No Content (empty body — success)
```

---

## 9. API Reference

### Base URL: `http://localhost:9090/api/tasks`

| Method | Endpoint | Request Body | Success | Error |
|---|---|---|---|---|
| GET | `/api/tasks` | None | 200 + JSON array | — |
| GET | `/api/tasks/{id}` | None | 200 + single task | 404 if not found |
| POST | `/api/tasks` | TaskRequestDTO | 200 + created task (with id) | 400 if validation fails |
| PUT | `/api/tasks/{id}` | TaskRequestDTO | 200 + updated task | 404 / 400 |
| DELETE | `/api/tasks/{id}` | None | 204 No Content | 404 if not found |

### Request Body (POST and PUT)

```json
{
  "title": "Learn Spring Boot",
  "description": "Mastering the basics",
  "completed": false
}
```

Rules:
- `title` — required, 3–100 characters
- `description` — optional, max 500 characters
- `completed` — required, must be `true` or `false`
- Do **NOT** send `id` or `completionStatus` — these are server-managed

### Response Body (GET / POST / PUT)

```json
{
  "id": 1,
  "title": "Learn Spring Boot",
  "description": "Mastering the basics",
  "completed": false,
  "completionStatus": "PENDING"
}
```

### Error Response (400 / 404 / 500)

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
# Get all tasks
curl http://localhost:9090/api/tasks

# Get task by ID
curl http://localhost:9090/api/tasks/1

# Create a new task
curl -X POST http://localhost:9090/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Spring Boot","description":"Mastering the basics","completed":false}'

# Update a task
curl -X PUT http://localhost:9090/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Learn Spring Boot","description":"Advanced topics","completed":true}'

# Delete a task
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

// GOOD — Spring injects the dependency; easy to swap in a mock during tests
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;  // Spring provides this at startup
}
```

### Optional\<T\> — Handling "might not exist"

```java
// findById() returns Optional<Task> — it may or may not contain a value

// BAD — throws a generic NoSuchElementException with no useful message
Task task = taskRepository.findById(id).get();

// GOOD — throws your custom exception, caught by GlobalExceptionHandler -> clean 404
Task task = taskRepository.findById(id)
        .orElseThrow(() -> new TaskNotFoundException(id));
```

### @Transactional

```java
@Transactional
public TaskResponseDTO createTask(TaskRequestDTO dto) {
    Task entity = taskMapper.toEntity(dto);
    Task saved  = taskRepository.save(entity);  // If this throws, everything is rolled back
    return taskMapper.toDTO(saved);
}
```

### boolean (primitive) vs Boolean (wrapper) — and Lombok

| Field declaration | Lombok generates |
|---|---|
| `private boolean completed;` | `isCompleted()` |
| `private Boolean completed;` | `getCompleted()` |

In both `Task.java` and `TaskRequestDTO.java`, `completed` is declared as `boolean` (primitive),
so Lombok generates `isCompleted()`. This is why `TaskService.updateTask()` calls
`requestDto.isCompleted()` — **not** `requestDto.getCompleted()`.

### Lombok Quick Reference

| Annotation | Generated code |
|---|---|
| `@Getter` | `getField()` for every field; `isField()` for booleans |
| `@Setter` | `setField(value)` for every field |
| `@NoArgsConstructor` | `public Task() {}` |
| `@AllArgsConstructor` | `public Task(Long id, String header, ...)` |
| `@Data` | `@Getter + @Setter + @ToString + @EqualsAndHashCode` |
| `@RequiredArgsConstructor` | Constructor for all `final` fields only |

### Spring Annotations Quick Reference

| Annotation | Meaning |
|---|---|
| `@SpringBootApplication` | Entry point — enables auto-config and component scan |
| `@RestController` | HTTP controller that returns JSON |
| `@RequestMapping` | Base URL path for the class |
| `@GetMapping` | Handle HTTP GET |
| `@PostMapping` | Handle HTTP POST |
| `@PutMapping` | Handle HTTP PUT |
| `@DeleteMapping` | Handle HTTP DELETE |
| `@PathVariable` | Extract `{id}` from the URL |
| `@RequestBody` | Parse JSON body into a Java object |
| `@Valid` | Trigger Bean Validation on the annotated parameter |
| `@Service` | Business logic bean |
| `@Repository` | Data access bean |
| `@Entity` | JPA-managed database table |
| `@Transactional` | Wrap in a DB transaction — auto-rollback on failure |
| `@RestControllerAdvice` | Global exception interceptor for all controllers |
| `@ExceptionHandler` | Method handles a specific exception type |

---

## 11. How Everything Connects

```
                       TaskManagerApplication.java
                                  |
                    @SpringBootApplication scans everything
                                  |
         +------------------------+------------------------+
         |                        |                        |
         v                        v                        v
 TaskController.java    GlobalExceptionHandler.java   application.yaml
 (@RestController)      (@RestControllerAdvice)       (port, DB, logging)
         |                        |
         | calls                  | intercepts exceptions
         v                        | thrown by any controller
 TaskService.java
 (@Service)
         |
   +-----+------+
   |             |
   v             v
TaskRepository  TaskMapper
(@Repository)   (@Mapper — MapStruct)
   |             |
   | SQL         | converts between
   v             v
PostgreSQL     Task (Entity) <-> TaskRequestDTO / TaskResponseDTO
(tasks table)
```

**Data types flowing between layers:**

```
HTTP JSON (request)  ->  TaskRequestDTO   [Controller — @Valid enforced here]
TaskRequestDTO       ->  Task (Entity)    [Service — via TaskMapper.toEntity()]
Task (Entity)        ->  PostgreSQL       [Repository — SQL via Hibernate]
PostgreSQL           ->  Task (Entity)    [Repository — Hibernate maps result set]
Task (Entity)        ->  TaskResponseDTO  [Service — via TaskMapper.toDTO()]
TaskResponseDTO      ->  HTTP JSON        [Controller — Jackson serializes]
```

**The Three Rules of this Architecture:**

1. **Controllers** accept and return DTOs — they never touch Entities or the database directly
2. **Services** contain all business logic — they convert between DTOs and Entities via the Mapper
3. **Repositories** only deal with Entities — they know nothing about DTOs or HTTP

Following these rules makes every layer independently:
- **Testable** — mock the layer below, test each layer in isolation
- **Changeable** — swap PostgreSQL for MongoDB without touching the Controller
- **Understandable** — every file has one clear, single responsibility

