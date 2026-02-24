# TaskManagerApplication — Design Document & Tutorial

> **Last Updated:** February 24, 2026
> **Spring Boot Version:** 4.0.3 | **Java Version:** 25 | **Database:** PostgreSQL

---

## Table of Contents

1. [What This Application Does](#1-what-this-application-does)
2. [Technology Stack](#2-technology-stack)
3. [Project Structure](#3-project-structure)
4. [Architecture Overview](#4-architecture-overview)
5. [Layer-by-Layer Deep Dive](#5-layer-by-layer-deep-dive)
   - 5.1 [Entry Point — TaskManagerApplication.java](#51-entry-point--taskmanagerapplicationjava)
   - 5.2 [Configuration — application.yaml](#52-configuration--applicationyaml)
   - 5.3 [Model Layer — Task.java](#53-model-layer--taskjava)
   - 5.4 [Repository Layer — TaskRepository.java](#54-repository-layer--taskrepositoryj)
   - 5.5 [DTO Layer — TaskRequestDTO & TaskResponseDTO](#55-dto-layer--taskrequestdto--taskresponsedto)
   - 5.6 [Mapper Layer — TaskMapper.java](#56-mapper-layer--taskmapperjava)
   - 5.7 [Service Layer — TaskService.java](#57-service-layer--taskservicejava)
   - 5.8 [Sanitization — SanitizationService.java](#58-sanitization--sanitizationservicejava)
   - 5.9 [Controller Layer — TaskController.java](#59-controller-layer--taskcontrollerjava)
   - 5.10 [Exception Handling](#510-exception-handling)
6. [Data Flow — End-to-End Request Walkthrough](#6-data-flow--end-to-end-request-walkthrough)
7. [API Reference](#7-api-reference)
8. [Key Design Decisions](#8-key-design-decisions)
9. [Build System — build.gradle.kts](#9-build-system--buildgradlekts)
10. [How to Run the Application](#10-how-to-run-the-application)

---

## 1. What This Application Does

TaskManagerApplication is a **RESTful API** built with Spring Boot that allows you to manage a list of tasks. It supports:

- **Creating** a new task
- **Reading** one task or all tasks
- **Updating** an existing task
- **Deleting** a task

Each task has a title, a description, and a completion status (`true`/`false`). The API also computes a human-readable `completionStatus` field (`"DONE"` or `"PENDING"`) and returns it in responses.

---

## 2. Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Language | Java 25 | Core programming language |
| Framework | Spring Boot 4.0.3 | Auto-configuration, DI, REST |
| Web | Spring MVC | HTTP request handling |
| Persistence | Spring Data JPA + Hibernate | ORM — Java objects ↔ DB rows |
| Database | PostgreSQL (Docker) | Persistent data storage |
| Mapping | MapStruct 1.6.0.Beta1 | Type-safe DTO ↔ Entity conversion |
| Code Gen | Lombok | Eliminates boilerplate (getters, constructors) |
| Validation | Jakarta Validation (Hibernate Validator) | Input validation (`@NotBlank`, `@Size`) |
| Security | OWASP AntiSamy 1.7.4 | XSS input sanitization |
| Build | Gradle (Kotlin DSL) | Dependency management, build |
| Logging | SLF4J + Logback | Structured log output to file + console |

---

## 3. Project Structure

```
src/main/java/com/taskmanager/app/
│
├── TaskManagerApplication.java        ← Entry point (@SpringBootApplication)
│
├── config/                            ← (reserved for future config beans)
│
├── controller/
│   └── TaskController.java            ← HTTP endpoints, routes requests to service
│
├── dto/
│   ├── TaskRequestDTO.java            ← What clients SEND (POST/PUT body)
│   └── TaskResponseDTO.java           ← What clients RECEIVE (GET/POST/PUT response)
│
├── exception/
│   ├── TaskNotFoundException.java     ← Thrown when task ID doesn't exist (→ 404)
│   ├── DuplicateTaskException.java    ← Thrown when same active title exists (→ 409)
│   ├── ErrorResponse.java             ← Structured JSON error body
│   └── GlobalExceptionHandler.java    ← Catches all exceptions across all controllers
│
├── mapper/
│   └── TaskMapper.java                ← MapStruct interface: DTO ↔ Entity
│
├── model/
│   └── Task.java                      ← JPA entity mapped to "tasks" DB table
│
├── repository/
│   └── TaskRepository.java            ← Spring Data JPA — auto-generated DB queries
│
└── service/
    ├── TaskService.java               ← Business logic (validation, orchestration)
    └── SanitizationService.java       ← XSS sanitization using OWASP AntiSamy

src/main/resources/
└── application.yaml                   ← DB config, server port, logging config

src/test/java/com/taskmanager/app/
└── TaskManagerApplicationTests.java   ← Spring context load test
```

---

## 4. Architecture Overview

This application follows a strict **layered (n-tier) architecture**. Each layer has one responsibility and communicates only with the layer directly below it.

```
┌─────────────────────────────────────────────────────┐
│                    CLIENT                           │
│     (curl / Postman / Browser / Frontend)           │
└────────────────────┬────────────────────────────────┘
                     │  HTTP Request (JSON)
                     ▼
┌─────────────────────────────────────────────────────┐
│              CONTROLLER LAYER                       │
│            TaskController.java                      │
│  • Receives HTTP requests                           │
│  • Triggers @Valid input validation                 │
│  • Delegates to Service — never contains logic      │
└────────────────────┬────────────────────────────────┘
                     │  TaskRequestDTO
                     ▼
┌─────────────────────────────────────────────────────┐
│               SERVICE LAYER                         │
│   TaskService.java + SanitizationService.java       │
│  • Sanitizes input (XSS removal)                    │
│  • Applies business rules (duplicate check)         │
│  • Orchestrates: calls Mapper, Repository           │
└──────┬──────────────────────┬───────────────────────┘
       │                      │
       ▼                      ▼
┌─────────────┐    ┌──────────────────────────────────┐
│  MAPPER     │    │         REPOSITORY LAYER          │
│  TaskMapper │    │       TaskRepository.java         │
│  (MapStruct)│    │  • Extends JpaRepository          │
│  DTO↔Entity │    │  • All DB queries auto-generated  │
└─────────────┘    └──────────────┬───────────────────┘
                                  │  SQL (via Hibernate)
                                  ▼
                   ┌──────────────────────────────────┐
                   │           DATABASE                │
                   │     PostgreSQL — "tasks" table    │
                   └──────────────────────────────────┘

         ┌──────────────────────────────────────────────┐
         │         CROSS-CUTTING CONCERNS               │
         │                                              │
         │  GlobalExceptionHandler  — catches all       │
         │  exceptions from any layer and converts      │
         │  them to structured JSON error responses.    │
         └──────────────────────────────────────────────┘
```

---

## 5. Layer-by-Layer Deep Dive

### 5.1 Entry Point — `TaskManagerApplication.java`

```java
@SpringBootApplication
public class TaskManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskManagerApplication.class, args);
    }
}
```

`@SpringBootApplication` is a convenience annotation combining three annotations:
- `@Configuration` — this class can define Spring beans
- `@EnableAutoConfiguration` — Spring Boot automatically configures everything it detects (e.g., JPA, Web MVC, DataSource)
- `@ComponentScan` — scans all sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller` beans

---

### 5.2 Configuration — `application.yaml`

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
      ddl-auto: update          # Hibernate auto-creates/updates the "tasks" table
    show-sql: false             # Disabled — SQL goes to log file via logger, not stdout
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 9090                    # API runs at http://localhost:9090

logging:
  file:
    name: logs/app.log          # All logs written here
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 7
      file-name-pattern: logs/app-%d{yyyy-MM-dd}.%i.log
  level:
    root: INFO
    com.example.TaskManagerApplication: DEBUG
    org.hibernate.SQL: DEBUG            # Logs SQL queries to the log file
    org.hibernate.orm.jdbc.bind: TRACE  # Logs bound parameter values
```

**Key points:**
- `ddl-auto: update` — Hibernate inspects the `Task` entity and creates/alters the `tasks` table automatically on startup. Never use `create` or `create-drop` in production (it wipes data).
- `show-sql: false` — we use `org.hibernate.SQL: DEBUG` instead, which routes SQL to the log file.
- `port: 9090` — the app listens on port 9090 (not the default 8080).

---

### 5.3 Model Layer — `Task.java`

```java
@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "title")
    private String header;        // Java field name differs from DB column name!

    private String description;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean completed;
}
```

**Important design detail — `header` vs `title`:**

The Java field is named `header` but maps to the DB column `title`. This is an intentional demonstration of the `@Column(name = "title")` mapping. It shows that Java field names and DB column names do NOT have to match — MapStruct handles the translation between the Java world and the client-facing DTO world.

| Java field | DB column | Client-facing name |
|---|---|---|
| `header` | `title` | `title` |
| `description` | `description` | `description` |
| `completed` | `completed` | `completed` |

**Lombok annotations:**
- `@Data` — generates getters, setters, `toString()`, `equals()`, `hashCode()`
- `@NoArgsConstructor` — generates `Task()` — **required by JPA** (Hibernate needs to instantiate entities with no-args constructor via reflection)
- `@AllArgsConstructor` — generates `Task(Long id, String header, String description, boolean completed)`

---

### 5.4 Repository Layer — `TaskRepository.java`

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByHeaderAndCompletedFalse(String header);
}
```

`JpaRepository<Task, Long>` gives you all these methods **for free** — no implementation needed:

| Method | SQL Equivalent |
|---|---|
| `findAll()` | `SELECT * FROM tasks` |
| `findById(id)` | `SELECT * FROM tasks WHERE id = ?` |
| `save(task)` | `INSERT INTO tasks ...` or `UPDATE tasks ...` |
| `delete(task)` | `DELETE FROM tasks WHERE id = ?` |
| `existsById(id)` | `SELECT COUNT(*) > 0 FROM tasks WHERE id = ?` |

**Custom method — `existsByHeaderAndCompletedFalse`:**

Spring Data JPA parses this method name and generates the query automatically:
```sql
SELECT COUNT(*) > 0 FROM tasks WHERE title = ? AND completed = false
```

This is called **Query Derivation** — Spring reads the method name like a sentence:
- `existsBy` → `SELECT COUNT(*) > 0 FROM tasks WHERE`
- `Header` → `title = ?` (using the DB column name)
- `And` → `AND`
- `CompletedFalse` → `completed = false`

---

### 5.5 DTO Layer — `TaskRequestDTO` & `TaskResponseDTO`

**Why DTOs?** The `Task` entity belongs to the database world. Exposing it directly to clients creates problems:
- Client can send an `id` and try to override DB-generated IDs
- You expose internal DB structure (e.g., the `header` field naming)
- You can't add computed fields like `completionStatus` without polluting the entity

#### `TaskRequestDTO` — what clients SEND

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Completion status must be specified")
    private Boolean completed;    // Boolean wrapper — so @NotNull actually works
}
```

**Why `Boolean` (wrapper) instead of `boolean` (primitive)?**
- Primitive `boolean` can **never** be `null` — it always defaults to `false`
- `@NotNull` on a primitive has **no effect** — the field is never null, so validation never triggers
- `Boolean` wrapper **can** be null — if the client omits `"completed"`, it stays `null`, and `@NotNull` correctly returns a 400 error

#### `TaskResponseDTO` — what clients RECEIVE

```java
@Data
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private boolean completed;
    private String completionStatus;    // Computed by the server — NOT in DB
}
```

`completionStatus` is a **server-computed field** that does not exist in the database. MapStruct computes it during mapping using a Java expression (`"DONE"` or `"PENDING"`).

#### DTO Comparison

| Field | TaskRequestDTO | TaskResponseDTO | DB Table |
|---|---|---|---|
| `id` | ❌ Not present | ✅ Present | ✅ Auto-generated |
| `title` | ✅ Required | ✅ Present | ✅ (stored as `title`) |
| `description` | ✅ Optional | ✅ Present | ✅ |
| `completed` | ✅ Required (Boolean) | ✅ Present (boolean) | ✅ |
| `completionStatus` | ❌ Not present | ✅ Computed | ❌ Not in DB |

---

### 5.6 Mapper Layer — `TaskMapper.java`

```java
@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(source = "header", target = "title")
    @Mapping(
        target = "completionStatus",
        expression = "java(task.isCompleted() ? \"DONE\" : \"PENDING\")"
    )
    TaskResponseDTO toDTO(Task task);

    @Mapping(source = "title", target = "header")
    @Mapping(target = "id", ignore = true)
    Task toEntity(TaskRequestDTO dto);
}
```

**MapStruct** generates a real Java implementation class at compile time (you can see it at `build/generated/sources/annotationProcessor/.../TaskMapperImpl.java`). It is NOT reflection-based — it's plain Java, so it's fast and type-safe.

**What the generated code looks like (simplified):**

```java
// This is AUTO-GENERATED by MapStruct at compile time
@Component
public class TaskMapperImpl implements TaskMapper {

    @Override
    public TaskResponseDTO toDTO(Task task) {
        if (task == null) return null;
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setTitle(task.getHeader());         // header → title
        dto.setId(task.getId());
        dto.setDescription(task.getDescription());
        dto.setCompleted(task.isCompleted());
        dto.setCompletionStatus(task.isCompleted() ? "DONE" : "PENDING");  // computed
        return dto;
    }

    @Override
    public Task toEntity(TaskRequestDTO dto) {
        if (dto == null) return null;
        Task task = new Task();
        task.setHeader(dto.getTitle());          // title → header
        // id is ignored — DB generates it
        task.setDescription(dto.getDescription());
        task.setCompleted(dto.getCompleted());
        return task;
    }
}
```

**`componentModel = "spring"`** makes MapStruct register `TaskMapperImpl` as a Spring bean, so it can be `@Autowired` / injected via constructor injection into `TaskService`.

---

### 5.7 Service Layer — `TaskService.java`

The service layer is the **brain** of the application. It:
1. Sanitizes input (delegates to `SanitizationService`)
2. Applies business rules
3. Orchestrates the mapper and repository

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final SanitizationService sanitizationService;

    public List<TaskResponseDTO> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(taskMapper::toDTO)
                .toList();
    }

    public TaskResponseDTO getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return taskMapper.toDTO(task);
    }

    @Transactional
    public TaskResponseDTO createTask(TaskRequestDTO requestDto) {
        sanitizeRequest(requestDto);                                  // 1. XSS clean
        if (taskRepository.existsByHeaderAndCompletedFalse(          // 2. Duplicate check
                requestDto.getTitle())) {
            throw new DuplicateTaskException(
                "You already have an active task with this title!");
        }
        Task taskEntity = taskMapper.toEntity(requestDto);           // 3. DTO → Entity
        Task savedTask  = taskRepository.save(taskEntity);           // 4. Save to DB
        return taskMapper.toDTO(savedTask);                          // 5. Entity → DTO
    }

    @Transactional
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDto) {
        sanitizeRequest(requestDto);                                  // 1. XSS clean
        Task existingTask = taskRepository.findById(id)              // 2. Find or 404
                .orElseThrow(() -> new TaskNotFoundException(id));
        existingTask.setHeader(requestDto.getTitle());               // 3. Apply changes
        existingTask.setDescription(requestDto.getDescription());
        existingTask.setCompleted(requestDto.getCompleted());
        Task updatedTask = taskRepository.save(existingTask);        // 4. Save
        return taskMapper.toDTO(updatedTask);                        // 5. Entity → DTO
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.delete(task);
    }

    private void sanitizeRequest(TaskRequestDTO requestDto) {
        requestDto.setTitle(sanitizationService.sanitize(requestDto.getTitle()));
        requestDto.setDescription(sanitizationService.sanitize(requestDto.getDescription()));
    }
}
```

**Why `@Transactional`?**

`@Transactional` wraps the method in a database transaction. If anything fails mid-way (e.g., a DB constraint violation after the save), the entire operation is rolled back — no partial data is committed.

- `getAllTasks()` and `getTaskById()` don't need `@Transactional` because they only read data.
- `createTask`, `updateTask`, `deleteTask` modify data, so they need it.

**Why `getAllTasks` returns `.toList()` (not the raw list)?**

`taskRepository.findAll()` returns a reference to an internal list. Returning it directly would expose internal state. `.toList()` creates a **new, unmodifiable copy** — safe defensive programming.

---

### 5.8 Sanitization — `SanitizationService.java`

**What problem does it solve?**

Without sanitization, an attacker can send:
```json
{ "title": "<script>alert('Hacked!')</script>Buy groceries" }
```
This gets stored in the DB. If a frontend ever renders it, the script runs in the user's browser — this is **XSS (Cross-Site Scripting)**.

**How AntiSamy works:**

AntiSamy parses the input against a **policy file** that defines what HTML tags are allowed. Our policy (`antisamy-slashdot.xml`) is the strictest bundled one — it allows **almost nothing**, which is exactly what we want for plain-text fields.

```
Input:  "<script>alert('x')</script>Buy groceries"
            ↓
        AntiSamy scans against antisamy-slashdot.xml
            ↓
Output: "Buy groceries"   ← dangerous parts stripped, plain text preserved
```

**Correct package name:** The library JAR puts classes under `org.owasp.validator.html` (not `org.owasp.antisamy`).

**Available policy files** (bundled in the AntiSamy jar):

| Policy File | Strictness | Allows |
|---|---|---|
| `antisamy-slashdot.xml` | 🔴 Strictest | Almost nothing — plain text only |
| `antisamy-myspace.xml` | 🟡 Medium | Basic formatting (`<b>`, `<i>`, `<u>`) |
| `antisamy-tinymce.xml` | 🟢 Permissive | Rich text editor content |
| `antisamy.xml` | 🟡 Medium | General purpose |

**Why sanitize before business validation?**

Because you want to validate the **clean** value, not the raw malicious one. Example:
- Attacker sends: `"<script></script>AB"` (16 chars raw, but only 2 chars clean)
- After sanitization: `"AB"`
- `@Size(min=3)` correctly rejects it — only 2 characters remain

---

### 5.9 Controller Layer — `TaskController.java`

```java
@RestController
@RequestMapping("/api/tasks")
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
        return ResponseEntity.noContent().build();  // 204 No Content
    }
}
```

**Key annotations explained:**

| Annotation | What it does |
|---|---|
| `@RestController` | Combines `@Controller` + `@ResponseBody`. Return values are automatically serialized to JSON. |
| `@RequestMapping("/api/tasks")` | All endpoints in this class are prefixed with `/api/tasks` |
| `@GetMapping` | Maps `GET /api/tasks` |
| `@GetMapping("/{id}")` | Maps `GET /api/tasks/5` → `id = 5` |
| `@PostMapping` | Maps `POST /api/tasks` |
| `@PutMapping("/{id}")` | Maps `PUT /api/tasks/5` |
| `@DeleteMapping("/{id}")` | Maps `DELETE /api/tasks/5` |
| `@PathVariable` | Extracts `{id}` from the URL |
| `@RequestBody` | Deserializes the JSON request body into `TaskRequestDTO` |
| `@Valid` | Triggers Jakarta Validation on the `TaskRequestDTO` fields |

**Why does `deleteTask` return `ResponseEntity<Void>` while others return DTOs?**

A successful DELETE has nothing to return — there is no task anymore. `ResponseEntity.noContent().build()` sends HTTP **204 No Content**, which is the correct REST convention for a successful delete with no body.

---

### 5.10 Exception Handling

#### Custom Exceptions

```
TaskNotFoundException    extends RuntimeException   → HTTP 404
DuplicateTaskException   extends RuntimeException   → HTTP 409
```

Both extend `RuntimeException` (unchecked), so you don't need `throws` declarations — they propagate up naturally to the `GlobalExceptionHandler`.

```java
// TaskNotFoundException
public TaskNotFoundException(Long id) {
    super("Task with ID " + id + " not found");
}

// DuplicateTaskException
public DuplicateTaskException(String message) {
    super(message);
}
```

#### `ErrorResponse.java` — Structured Error Body

Instead of returning a plain string or Spring's default error JSON, all errors return this structured object:

```java
@Getter @Builder @AllArgsConstructor
public class ErrorResponse {
    private final int status;           // e.g. 404
    private final String error;         // e.g. "Not Found"
    private final String message;       // e.g. "Task with ID 999 not found"
    private Map<String, String> errors; // Field-level errors for validation failures
    private final LocalDateTime timestamp;
}
```

Example JSON output:
```json
{
    "status": 404,
    "error": "Not Found",
    "message": "Task with ID 999 not found",
    "timestamp": "2026-02-24T10:30:00"
}
```

For validation failures, the `errors` map is populated:
```json
{
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed for one or more fields",
    "errors": {
        "title": "Title is required",
        "completed": "Completion status must be specified"
    },
    "timestamp": "2026-02-24T10:30:00"
}
```

#### `GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) { ... }  // → 404

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(...) { ... }  // → 400

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(...) { ... }  // → 400

    @ExceptionHandler(DuplicateTaskException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTask(...) { ... }  // → 409

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) { ... }  // → 500
}
```

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`. Spring registers this as a **global interceptor** that wraps all controllers. When any controller (or anything it calls) throws an exception:

1. Spring stops normal execution
2. Searches `GlobalExceptionHandler` for a matching `@ExceptionHandler`
3. Most specific match wins (`TaskNotFoundException` is matched before the generic `Exception`)
4. The handler builds an `ErrorResponse` and returns it with the right HTTP status

**The 500 handler NEVER exposes `ex.getMessage()` to the client** — internal errors could reveal sensitive stack traces. It returns a generic safe message instead.

---

## 6. Data Flow — End-to-End Request Walkthrough

### POST `/api/tasks` — Creating a Task

```
Client sends:
POST http://localhost:9090/api/tasks
{
  "title": "Learn Spring Boot",
  "description": "Mastering the basics",
  "completed": false
}

Step 1 — TaskController.createTask()
  @Valid triggers Jakarta validation on TaskRequestDTO:
  ✓ title = "Learn Spring Boot" → passes @NotBlank, @Size(min=3, max=100)
  ✓ description = "Mastering..." → passes @Size(max=500)
  ✓ completed = false → passes @NotNull
  If any fails → MethodArgumentNotValidException → GlobalExceptionHandler → 400

Step 2 — TaskService.createTask()
  2a. sanitizeRequest() is called
      → SanitizationService.sanitize("Learn Spring Boot")
      → AntiSamy scans against antisamy-slashdot.xml
      → "Learn Spring Boot" (no HTML found — returned unchanged)

  2b. Duplicate check:
      → taskRepository.existsByHeaderAndCompletedFalse("Learn Spring Boot")
      → SQL: SELECT COUNT(*) > 0 FROM tasks WHERE title = 'Learn Spring Boot' AND completed = false
      → false (no duplicate) — proceed

  2c. taskMapper.toEntity(requestDto):
      → Creates Task { id=null, header="Learn Spring Boot", description="...", completed=false }

  2d. taskRepository.save(task):
      → SQL: INSERT INTO tasks (title, description, completed) VALUES (?, ?, ?)
      → DB assigns id=1

  2e. taskMapper.toDTO(savedTask):
      → Creates TaskResponseDTO {
            id=1,
            title="Learn Spring Boot",    ← from task.header
            description="Mastering...",
            completed=false,
            completionStatus="PENDING"    ← computed: false → "PENDING"
         }

Step 3 — Response:
HTTP 200 OK
{
  "id": 1,
  "title": "Learn Spring Boot",
  "description": "Mastering the basics",
  "completed": false,
  "completionStatus": "PENDING"
}
```

### GET `/api/tasks/999` — Task Not Found

```
Step 1 — TaskController.getTaskById(999)

Step 2 — TaskService.getTaskById(999)
  → taskRepository.findById(999)
  → SQL: SELECT * FROM tasks WHERE id = 999
  → Returns Optional.empty()
  → .orElseThrow() → throws TaskNotFoundException("Task with ID 999 not found")

Step 3 — GlobalExceptionHandler.handleTaskNotFound()
  → Builds ErrorResponse { status=404, error="Not Found", message="Task with ID 999 not found" }

Step 4 — Response:
HTTP 404 Not Found
{
  "status": 404,
  "error": "Not Found",
  "message": "Task with ID 999 not found",
  "timestamp": "2026-02-24T10:30:00"
}
```

---

## 7. API Reference

**Base URL:** `http://localhost:9090`

### GET `/api/tasks`
Returns all tasks.

**Response 200:**
```json
[
  {
    "id": 1,
    "title": "Learn Spring Boot",
    "description": "Mastering the basics",
    "completed": false,
    "completionStatus": "PENDING"
  }
]
```

---

### GET `/api/tasks/{id}`
Returns a single task by ID.

```
GET /api/tasks/1
```

**Response 200:** Same as single task object above.

**Response 404:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Task with ID 999 not found",
  "timestamp": "2026-02-24T10:30:00"
}
```

---

### POST `/api/tasks`
Creates a new task.

**Request:**
```bash
curl --location 'http://localhost:9090/api/tasks' \
--header 'Content-Type: application/json' \
--data '{
  "title": "Learn Spring Boot",
  "description": "Mastering the basics",
  "completed": false
}'
```

**Response 200:** Created task with DB-assigned `id` and computed `completionStatus`.

**Response 400 (validation failure):**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "errors": {
    "title": "Title is required"
  },
  "timestamp": "2026-02-24T10:30:00"
}
```

**Response 409 (duplicate title):**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "You already have an active task with this title!",
  "timestamp": "2026-02-24T10:30:00"
}
```

---

### PUT `/api/tasks/{id}`
Updates an existing task.

```bash
curl --location --request PUT 'http://localhost:9090/api/tasks/1' \
--header 'Content-Type: application/json' \
--data '{
  "title": "Learn Spring Boot",
  "description": "Updated description",
  "completed": true
}'
```

**Response 200:** Updated task with `completionStatus: "DONE"`.

---

### DELETE `/api/tasks/{id}`

```bash
curl --location --request DELETE 'http://localhost:9090/api/tasks/1'
```

**Response 204 No Content** (empty body).

---

## 8. Key Design Decisions

### Decision 1: Separate `TaskRequestDTO` and `TaskResponseDTO`

| Without DTOs | With DTOs |
|---|---|
| Client can send `id` and tamper with it | `id` is absent from request DTO — impossible to send |
| Internal field name `header` exposed to client | Client sees `title` — clean public API |
| Can't add `completionStatus` without DB change | Computed server-side in mapper |
| Changing DB schema breaks client contract | DTO is a stable interface; entity can change |

### Decision 2: `Boolean` wrapper (not `boolean` primitive) in `TaskRequestDTO`

```java
// ❌ Wrong — @NotNull has NO effect on primitives
private boolean completed;    // always defaults to false, never null

// ✅ Correct — @NotNull works on wrapper types
private Boolean completed;    // can be null → @NotNull triggers 400 if omitted
```

### Decision 3: Sanitization in Service Layer (not DTO)

DTOs are plain data holders. Business logic — including input cleaning — belongs in the service layer. This keeps:
- DTOs simple and reusable
- A single place to change if we switch sanitization libraries
- The service fully testable without DTO knowledge

### Decision 4: Sanitize BEFORE business validation

```
sanitizeRequest(dto)    ← strip HTML first
     ↓
existsByHeader(...)     ← duplicate check on CLEAN title
     ↓
save()
```

If we duplicated-checked the raw input, `<b>Buy groceries</b>` would bypass the duplicate check against `Buy groceries`.

### Decision 5: `existsByHeaderAndCompletedFalse` (not `existsByHeader`)

We check for duplicates only among **active (not completed)** tasks. Once a task is marked complete, you can create a new one with the same title. This models real-world task management where the same task can recur.

### Decision 6: Global Exception Handler — never expose `ex.getMessage()` for 500s

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    // ❌ DON'T: return ex.getMessage() — exposes internal DB errors, stack traces, paths
    // ✅ DO: return a safe generic message
    ErrorResponse error = new ErrorResponse(500, "Internal Server Error",
        "Something went wrong. Please try again later.");
    ...
}
```

---

## 9. Build System — `build.gradle.kts`

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.mapstruct:mapstruct:1.6.0.Beta1")
    implementation("org.owasp.antisamy:antisamy:1.7.4")

    compileOnly("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")

    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.0.Beta1")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

**Dependency scope explanation:**

| Scope | Meaning | Example |
|---|---|---|
| `implementation` | Available at compile time AND runtime. Packaged in the final jar. | `spring-boot-starter-web`, `antisamy` |
| `compileOnly` | Available at compile time ONLY. NOT in the final jar. | `lombok` — only needed to generate code; the generated code is what ships |
| `runtimeOnly` | NOT available at compile time. Only present when the app runs. | `postgresql` JDBC driver — your code compiles against the JPA abstraction |
| `annotationProcessor` | A tool that runs during compilation to generate code. Never in the jar. | `lombok`, `mapstruct-processor` — generate code before compilation |
| `testImplementation` | Available only during tests. | `spring-boot-starter-test` |

**Why `lombok-mapstruct-binding`?**

Both Lombok and MapStruct are annotation processors. If Lombok runs after MapStruct, MapStruct can't see the Lombok-generated getters/setters and fails. `lombok-mapstruct-binding` ensures **Lombok always runs before MapStruct**.

---

## 10. How to Run the Application

### Prerequisites

1. **Docker** running with a PostgreSQL container:
```bash
docker-compose up -d
```

The `docker-compose.yml` starts PostgreSQL on port `5432` with:
- Database: `taskdb`
- Username: `docker`
- Password: `docker`

2. **Java 25** installed (check with `java --version`)

### Running the App

```bash
# From the project root
./gradlew bootRun
```

The app starts at `http://localhost:9090`.

### Running in Debug Mode

```bash
./gradlew bootRun --debug-jvm
```

Then attach your IDE debugger to port `5005`.

Or in IntelliJ IDEA: **Run → Debug → TaskManagerApplication**

### Running Tests

```bash
./gradlew test
```

Test reports are generated at:
```
build/reports/tests/test/index.html
```

### Building a JAR

```bash
./gradlew build
```

The executable jar is at:
```
build/libs/TaskManagerApplication-0.0.1-SNAPSHOT.jar
```

Run it with:
```bash
java -jar build/libs/TaskManagerApplication-0.0.1-SNAPSHOT.jar
```

### Useful Endpoints

| Purpose | Command |
|---|---|
| Get all tasks | `curl http://localhost:9090/api/tasks` |
| Get task by ID | `curl http://localhost:9090/api/tasks/1` |
| Create task | `curl -X POST http://localhost:9090/api/tasks -H 'Content-Type: application/json' -d '{"title":"My Task","description":"Details","completed":false}'` |
| Update task | `curl -X PUT http://localhost:9090/api/tasks/1 -H 'Content-Type: application/json' -d '{"title":"Updated","description":"New","completed":true}'` |
| Delete task | `curl -X DELETE http://localhost:9090/api/tasks/1` |

### Log File

Application logs are written to:
```
logs/app.log
```

Rotated daily, max 10MB per file, last 7 files retained.

