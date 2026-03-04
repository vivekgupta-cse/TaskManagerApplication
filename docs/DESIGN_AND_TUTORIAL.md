# TaskManagerApplication — Complete Design Document & Tutorial

> **Target audience:** Someone learning Spring Boot who wants to understand not just *what* the code does, but *why* every decision was made.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Project Structure](#3-project-structure)
4. [Architecture: The Layered Design](#4-architecture-the-layered-design)
5. [Layer-by-Layer Deep Dive](#5-layer-by-layer-deep-dive)
   - 5.1 [Entry Point — `TaskManagerApplication.java`](#51-entry-point--taskmanagerapplicationjava)
   - 5.2 [Model Layer — `Task.java`](#52-model-layer--taskjava)
   - 5.3 [Repository Layer — `TaskRepository.java`](#53-repository-layer--taskrepositoryja)
   - 5.4 [DTOs — Request & Response](#54-dtos--request--response)
   - 5.5 [Mapper — `TaskMapper.java`](#55-mapper--taskmapperjava)
   - 5.6 [Service Layer — `TaskService.java`](#56-service-layer--taskservicejava)
   - 5.7 [Security Service — `SanitizationService.java`](#57-security-service--sanitizationservicejava)
   - 5.8 [Controller Layer — `TaskController.java`](#58-controller-layer--taskcontrollerjava)
   - 5.9 [Exception Handling](#59-exception-handling)
6. [Configuration](#6-configuration)
   - 6.1 [application.yaml (main)](#61-applicationyaml-main)
   - 6.2 [application.yaml (test)](#62-applicationyaml-test)
   - 6.3 [build.gradle.kts](#63-buildgradlekts)
   - 6.4 [docker-compose.yml](#64-docker-composeyml)
7. [Complete Request Lifecycle — Step by Step](#7-complete-request-lifecycle--step-by-step)
8. [Spring Boot Key Concepts Explained](#8-spring-boot-key-concepts-explained)
9. [Testing Strategy](#9-testing-strategy)
   - 9.1 [Test Types Used](#91-test-types-used)
   - 9.2 [TaskControllerTest](#92-taskcontrollertest)
   - 9.3 [TaskServiceTest](#93-taskservicetest)
   - 9.4 [SanitizationServiceTest](#94-sanitizationservicetest)
   - 9.5 [GlobalExceptionHandlerTest](#95-globalexceptionhandlertest)
   - 9.6 [TaskMapperTest](#96-taskmappertest)
   - 9.7 [TaskManagerApplicationTests](#97-taskmanagerapplicationtests)
10. [Code Coverage with JaCoCo](#10-code-coverage-with-jacoco)
11. [API Reference](#11-api-reference)
12. [Key Design Decisions Explained](#12-key-design-decisions-explained)
13. [Common Errors and How They Are Handled](#13-common-errors-and-how-they-are-handled)

---

## 1. Project Overview

**TaskManagerApplication** is a REST API built with Spring Boot that allows users to manage tasks (a to-do list backend). Users can:

- Create tasks with a title, optional description, and completion status.
- Read all tasks or a specific task by ID.
- Update any task.
- Delete a task.

The application protects against **XSS (Cross-Site Scripting)** attacks by sanitizing all user input before saving it. It also validates incoming data strictly and returns structured, human-readable error messages.

---

## 2. Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 25 | Programming language |
| **Spring Boot** | 4.0.3 | Web framework, auto-configuration |
| **Spring Data JPA** | (via Boot) | Database access via repositories |
| **Hibernate** | (via JPA) | ORM — maps Java classes to DB tables |
| **PostgreSQL** | 16 | Production database |
| **H2** | (test scope) | In-memory database used only during tests |
| **Lombok** | latest | Reduces boilerplate (getters, setters, constructors) |
| **MapStruct** | 1.6.0.Beta1 | Auto-generates type-safe object mapper code |
| **OWASP AntiSamy** | 1.7.4 | Sanitizes HTML/JS from user input (XSS protection) |
| **Jakarta Validation** | (via Boot) | Bean validation annotations (@NotBlank, @Size, @NotNull) |
| **JaCoCo** | (via Gradle plugin) | Code coverage measurement |
| **JUnit 5** | 6.0.3 | Unit test framework |
| **Mockito** | (via Boot Test) | Mocking collaborators in unit tests |
| **AssertJ** | (via Boot Test) | Fluent assertion library |
| **MockMvc** | (via Spring Test) | Testing HTTP endpoints without starting a server |
| **Gradle** | (wrapper) | Build tool |
| **Docker Compose** | — | Runs PostgreSQL in a container locally |

---

## 3. Project Structure

```
TaskManagerApplication/
├── build.gradle.kts                  ← Build config, dependencies, JaCoCo setup
├── docker-compose.yml                ← Starts PostgreSQL container for local dev
├── gradlew / gradlew.bat             ← Gradle wrapper scripts
│
├── src/
│   ├── main/
│   │   ├── java/com/taskmanager/app/
│   │   │   ├── TaskManagerApplication.java    ← Entry point (@SpringBootApplication)
│   │   │   ├── config/                        ← (backup only — H2ConsoleConfig)
│   │   │   ├── controller/
│   │   │   │   └── TaskController.java        ← HTTP endpoints
│   │   │   ├── dto/
│   │   │   │   ├── TaskRequestDTO.java        ← What client sends
│   │   │   │   └── TaskResponseDTO.java       ← What server sends back
│   │   │   ├── exception/
│   │   │   │   ├── DuplicateTaskException.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── TaskNotFoundException.java
│   │   │   ├── mapper/
│   │   │   │   └── TaskMapper.java            ← MapStruct interface
│   │   │   ├── model/
│   │   │   │   └── Task.java                  ← JPA Entity (maps to DB table)
│   │   │   ├── repository/
│   │   │   │   └── TaskRepository.java        ← Spring Data JPA repository
│   │   │   └── service/
│   │   │       ├── SanitizationService.java   ← XSS protection
│   │   │       └── TaskService.java           ← Business logic
│   │   └── resources/
│   │       └── application.yaml              ← Main config (PostgreSQL, port 9090)
│   │
│   └── test/
│       ├── java/com/taskmanager/app/
│       │   ├── TaskManagerApplicationTests.java
│       │   ├── controller/
│       │   │   └── TaskControllerTest.java
│       │   ├── exception/
│       │   │   └── GlobalExceptionHandlerTest.java
│       │   ├── mapper/
│       │   │   └── TaskMapperTest.java
│       │   └── service/
│       │       ├── SanitizationServiceTest.java
│       │       └── TaskServiceTest.java
│       └── resources/
│           └── application.yaml              ← Test config (H2 in-memory DB)
│
├── docs/
│   ├── DESIGN_AND_TUTORIAL.md        ← This file
│   └── GlobalExceptionHandlerDeepDive.md
└── logs/
    └── app.log                        ← Log file (auto-created at runtime)
```

---

## 4. Architecture: The Layered Design

This project follows the **Layered Architecture** pattern (also called N-tier architecture). Each layer has a single, clear responsibility, and each layer only talks to the layer directly below it.

```
┌──────────────────────────────────────────────────────────┐
│                    HTTP Client (browser/Postman/curl)     │
└──────────────────────────┬───────────────────────────────┘
                           │  HTTP Request (JSON)
                           ▼
┌──────────────────────────────────────────────────────────┐
│               CONTROLLER LAYER                           │
│   TaskController.java                                    │
│   • Receives HTTP requests                               │
│   • Validates request body (@Valid)                      │
│   • Calls Service layer                                  │
│   • Returns HTTP response (JSON)                         │
└──────────────────────────┬───────────────────────────────┘
                           │  calls
                           ▼
┌──────────────────────────────────────────────────────────┐
│               SERVICE LAYER                              │
│   TaskService.java + SanitizationService.java            │
│   • Business logic lives here                            │
│   • Sanitizes input (XSS protection)                     │
│   • Checks for duplicates                                │
│   • Uses mapper to convert between DTOs and Entities     │
│   • Calls Repository layer                               │
└──────────────────────────┬───────────────────────────────┘
                           │  calls
                           ▼
┌──────────────────────────────────────────────────────────┐
│               REPOSITORY LAYER                           │
│   TaskRepository.java                                    │
│   • Talks to the database                                │
│   • Uses Spring Data JPA (no SQL needed!)                │
└──────────────────────────┬───────────────────────────────┘
                           │  SQL (via Hibernate)
                           ▼
┌──────────────────────────────────────────────────────────┐
│               DATABASE                                   │
│   PostgreSQL (prod) / H2 (test)                          │
│   Table: "tasks"                                         │
└──────────────────────────────────────────────────────────┘
```

**Why layering?**
- **Separation of concerns**: Each layer does one thing only.
- **Testability**: You can test the Service independently of the database by mocking the Repository.
- **Maintainability**: Changing the database only affects the Repository layer; changing business rules only affects the Service layer.

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

**What `@SpringBootApplication` does** — it's a shortcut for three annotations:

| Annotation | What it does |
|---|---|
| `@SpringBootConfiguration` | Marks this as a Spring configuration class |
| `@EnableAutoConfiguration` | Tells Spring Boot to automatically configure beans based on your classpath (e.g., if you have JPA on the classpath, it auto-configures Hibernate) |
| `@ComponentScan` | Tells Spring to scan this package and all sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller`, etc. |

`SpringApplication.run(...)` boots the entire application: starts the embedded Tomcat server, loads configuration, creates all beans, connects to the database.

---

### 5.2 Model Layer — `Task.java`

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
    private String header;        // Java field named differently from DB column!

    private String description;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean completed;
}
```

**Key concepts:**

| Annotation | Meaning |
|---|---|
| `@Entity` | This class is a JPA-managed entity. Hibernate will create/manage the DB table. |
| `@Table(name = "tasks")` | The DB table is named `tasks` (not `task`). |
| `@Id` | This field is the primary key. |
| `@GeneratedValue(IDENTITY)` | The DB auto-increments the ID (1, 2, 3…). We never set it manually. |
| `@Column(nullable = false, name = "title")` | Maps Java field `header` to DB column `title`. The column cannot be NULL. |
| `@Data` (Lombok) | Auto-generates: getters, setters, `toString()`, `equals()`, `hashCode()`. |
| `@NoArgsConstructor` | JPA **requires** a no-argument constructor. Without it, Hibernate cannot instantiate objects. |
| `@AllArgsConstructor` | Generates a constructor with all fields — useful in test code. |

**Important design note:** The Java field is `header` but the DB column is `title`. This is intentional — it demonstrates that the Java model and database schema can differ. The mapper (`TaskMapper`) handles translating between them.

---

### 5.3 Repository Layer — `TaskRepository.java`

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    boolean existsByHeaderAndCompletedFalse(String header);
}
```

**This is the most "magical" part of Spring Boot.** `JpaRepository<Task, Long>` gives you these methods for free — you write zero SQL:

| Method | SQL it generates |
|---|---|
| `findAll()` | `SELECT * FROM tasks` |
| `findById(id)` | `SELECT * FROM tasks WHERE id = ?` |
| `save(task)` | `INSERT INTO tasks (...)` or `UPDATE tasks SET ...` |
| `delete(task)` | `DELETE FROM tasks WHERE id = ?` |
| `existsById(id)` | `SELECT COUNT(*) > 0 FROM tasks WHERE id = ?` |

**Custom query method:**
```java
boolean existsByHeaderAndCompletedFalse(String header);
```
Spring Data JPA **reads the method name** and generates the SQL:
```sql
SELECT COUNT(*) > 0 FROM tasks WHERE title = ? AND completed = false
```
The naming convention is: `existsBy` + `Header` (field name) + `And` + `CompletedFalse` (field = false).

---

### 5.4 DTOs — Request & Response

**DTO = Data Transfer Object.** DTOs are plain objects used to carry data between layers. They are NOT stored in the database.

**Why have separate DTOs instead of using the entity directly?**
- The entity might have fields you don't want to expose (e.g., internal flags, passwords).
- The client and server can evolve independently.
- You can add computed/derived fields (like `completionStatus`) that don't exist in the database.

#### TaskRequestDTO — What the client sends

```java
public class TaskRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Completion status must be specified")
    private Boolean completed;   // Boolean wrapper, not primitive boolean
}
```

**Validation annotations:**

| Annotation | What it checks |
|---|---|
| `@NotBlank` | The string is not null, not empty, and not just whitespace |
| `@Size(min, max)` | The string length is within bounds |
| `@NotNull` | The value is not null |

**Why `Boolean` (wrapper) instead of `boolean` (primitive)?**
- Primitive `boolean` defaults to `false` if the field is omitted in JSON.
- Wrapper `Boolean` defaults to `null` — so `@NotNull` can detect if the client forgot to send it.

#### TaskResponseDTO — What the server sends back

```java
@Data
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private boolean completed;
    private String completionStatus;  // "DONE" or "PENDING" — computed, not in DB
}
```

`completionStatus` is a **derived field** — it doesn't exist in the database. It's computed by the mapper based on the `completed` boolean. This is a clean way to enrich the response without polluting the database schema.

---

### 5.5 Mapper — `TaskMapper.java`

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

**MapStruct** is a code generator. You write just the interface, and MapStruct generates the actual implementation class (`TaskMapperImpl`) at **compile time**. You never write manual mapping code like:

```java
// You don't write this — MapStruct generates it:
dto.setTitle(task.getHeader());
dto.setCompletionStatus(task.isCompleted() ? "DONE" : "PENDING");
```

**Mapping rules explained:**

| Annotation | Meaning |
|---|---|
| `@Mapping(source = "header", target = "title")` | When converting `Task → ResponseDTO`, take `task.header` and put it in `dto.title` |
| `@Mapping(target = "completionStatus", expression = "java(...)")` | Compute `completionStatus` using a Java expression |
| `@Mapping(source = "title", target = "header")` | When converting `RequestDTO → Task`, take `dto.title` and put it in `task.header` |
| `@Mapping(target = "id", ignore = true)` | Never copy the ID from the request — the database generates it |
| `componentModel = "spring"` | Make `TaskMapperImpl` a Spring bean, so it can be `@Autowired` |

---

### 5.6 Service Layer — `TaskService.java`

This is the **brain** of the application. All business logic lives here.

```java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final SanitizationService sanitizationService;
    // ...
}
```

**All five operations:**

#### GET all tasks
```java
public List<TaskResponseDTO> getAllTasks() {
    return taskRepository.findAll()  // DB: SELECT * FROM tasks
            .stream()
            .map(taskMapper::toDTO)  // Each Task → TaskResponseDTO
            .toList();
}
```
Stream pipeline: database list → Java stream → map each element → collect to list.

#### GET one task
```java
public TaskResponseDTO getTaskById(Long id) {
    Task task = taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
    return taskMapper.toDTO(task);
}
```
`findById` returns `Optional<Task>`. `orElseThrow` either unwraps it or throws a `TaskNotFoundException`, which is caught by the `GlobalExceptionHandler` and returns a 404 response.

#### CREATE a task
```java
@Transactional
public TaskResponseDTO createTask(TaskRequestDTO requestDto) {
    sanitizeRequest(requestDto);  // Strip HTML/JS

    if (taskRepository.existsByHeaderAndCompletedFalse(requestDto.getTitle())) {
        throw new DuplicateTaskException("You already have an active task with this title!");
    }
    Task taskEntity = taskMapper.toEntity(requestDto);
    Task savedTask  = taskRepository.save(taskEntity);
    return taskMapper.toDTO(savedTask);
}
```
Step by step:
1. **Sanitize**: Remove any dangerous HTML or JavaScript from the input.
2. **Duplicate check**: Is there already an active (incomplete) task with the same title? If so, reject it with a 409 Conflict.
3. **Map**: Convert `RequestDTO → Task` entity (id is null at this point).
4. **Save**: `repository.save()` does `INSERT INTO tasks (...)` and returns the saved entity with the DB-generated id.
5. **Map back**: Convert the saved `Task → ResponseDTO` (now includes the real id).

#### UPDATE a task
```java
@Transactional
public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDto) {
    sanitizeRequest(requestDto);

    Task existingTask = taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));

    existingTask.setHeader(requestDto.getTitle());
    existingTask.setDescription(requestDto.getDescription());
    existingTask.setCompleted(requestDto.getCompleted());

    Task updatedTask = taskRepository.save(existingTask);
    return taskMapper.toDTO(updatedTask);
}
```
For update, we fetch the existing entity from the database first (or throw 404), apply the new values from the request, and save it back.

#### DELETE a task
```java
@Transactional
public void deleteTask(Long id) {
    Task task = taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
    taskRepository.delete(task);
}
```

**What is `@Transactional`?**
A transaction is a group of database operations that either ALL succeed or ALL fail together. If something throws an exception mid-way, Hibernate rolls back everything automatically. The `@Transactional` annotation tells Spring to wrap the method in a transaction.

---

### 5.7 Security Service — `SanitizationService.java`

**Problem being solved:** XSS (Cross-Site Scripting).

If a user sends: `"title": "<script>alert('Hacked!')</script>Buy groceries"` and you store it without sanitization, then any frontend that renders this text will execute the JavaScript — a security vulnerability.

**How AntiSamy works:**

```java
CleanResults results = antiSamy.scan(input, policy);
return results.getCleanHTML();
```

AntiSamy uses a **policy file** (`antisamy-slashdot.xml`) that defines what HTML is "safe". The slashdot policy is the strictest — it allows almost no HTML, stripping all tags. This is perfect for a plain-text field like task title.

```
Input:  "<script>alert('x')</script>Buy groceries"
Output: "Buy groceries"

Input:  "Buy groceries"     <- no dangerous content
Output: "Buy groceries"     <- unchanged
```

The service is designed to be **fail-safe**:
- If the policy file fails to load, it logs a warning but doesn't crash.
- If sanitization itself fails, it logs an error and returns the original input.

---

### 5.8 Controller Layer — `TaskController.java`

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    public List<TaskResponseDTO> getAllTasks() { ... }

    @GetMapping("/{id}")
    public TaskResponseDTO getTaskById(@PathVariable Long id) { ... }

    @PostMapping
    public TaskResponseDTO createTask(@Valid @RequestBody TaskRequestDTO requestDto) { ... }

    @PutMapping("/{id}")
    public TaskResponseDTO updateTask(@PathVariable Long id,
                                      @Valid @RequestBody TaskRequestDTO requestDto) { ... }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build(); // HTTP 204
    }
}
```

**Key annotations:**

| Annotation | Meaning |
|---|---|
| `@RestController` | This class handles HTTP requests. Return values are automatically serialized to JSON. |
| `@RequestMapping("/api/tasks")` | All endpoints in this class start with `/api/tasks`. |
| `@GetMapping` | Handles `GET /api/tasks` |
| `@GetMapping("/{id}")` | Handles `GET /api/tasks/5` — the `{id}` is a path variable. |
| `@PostMapping` | Handles `POST /api/tasks` |
| `@PutMapping("/{id}")` | Handles `PUT /api/tasks/5` |
| `@DeleteMapping("/{id}")` | Handles `DELETE /api/tasks/5` |
| `@PathVariable Long id` | Extracts `5` from `/api/tasks/5` and binds it to the `id` parameter. |
| `@RequestBody TaskRequestDTO` | Deserializes the JSON request body into a `TaskRequestDTO` object. |
| `@Valid` | Triggers Jakarta Bean Validation on the request body. If validation fails, throws `MethodArgumentNotValidException`. |

---

### 5.9 Exception Handling

Without exception handling, Spring Boot would return raw stack traces or generic error HTML pages. Instead, `GlobalExceptionHandler` intercepts all exceptions and returns structured JSON.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    ...
}
```

`@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody`. It's a global interceptor for all controllers.

**Exception → HTTP Status mapping:**

| Exception | HTTP Status | When thrown |
|---|---|---|
| `TaskNotFoundException` | 404 Not Found | Task with given ID doesn't exist |
| `MethodArgumentNotValidException` | 400 Bad Request | @Valid fails (e.g., title is blank) |
| `IllegalArgumentException` | 400 Bad Request | Invalid argument |
| `DuplicateTaskException` | 409 Conflict | Active task with same title already exists |
| `Exception` (catch-all) | 500 Internal Server Error | Any unexpected error |

**`ErrorResponse` structure:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Task with ID 42 not found",
  "timestamp": "2026-02-24T10:30:00"
}
```

For validation errors, the `errors` field is also populated:
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

---

## 6. Configuration

### 6.1 `application.yaml` (main)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskdb
    username: docker
    password: docker
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update       # Hibernate auto-creates/updates tables to match entities
    show-sql: false

server:
  port: 9090               # App runs on http://localhost:9090

logging:
  file:
    name: logs/app.log     # Logs written to a file (auto-rotated)
  level:
    org.hibernate.SQL: DEBUG  # Shows SQL queries in logs
```

**`ddl-auto: update`** — Hibernate compares your entities against the existing database schema and alters the schema to match. Good for development, but be careful in production (use `validate` or run proper migrations).

### 6.2 `application.yaml` (test)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop  # Create schema on start, drop on stop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

Tests use **H2** (a pure-Java in-memory database) instead of PostgreSQL. This means:
- No external PostgreSQL server needed to run tests.
- Tests run faster.
- `create-drop` ensures a fresh, clean database for every test run.

### 6.3 `build.gradle.kts`

Key dependencies:
```kotlin
implementation("org.springframework.boot:spring-boot-starter-web")           // REST API
implementation("org.springframework.boot:spring-boot-starter-data-jpa")      // JPA/Hibernate
implementation("org.springframework.boot:spring-boot-starter-validation")     // @Valid
compileOnly("org.projectlombok:lombok")                                        // Boilerplate reduction
annotationProcessor("org.projectlombok:lombok")
implementation("org.mapstruct:mapstruct:1.6.0.Beta1")                         // Object mapping
annotationProcessor("org.mapstruct:mapstruct-processor:1.6.0.Beta1")          // Code generation
annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")       // Lombok + MapStruct compatibility
implementation("org.owasp.antisamy:antisamy:1.7.4")                           // XSS protection
runtimeOnly("org.postgresql:postgresql")                                        // PostgreSQL driver
testRuntimeOnly("com.h2database:h2")                                           // H2 for tests
```

**JaCoCo configuration:**
```kotlin
plugins {
    jacoco   // Adds code coverage capability
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)  // Always generate report after tests
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)   // For CI/CD tools
        html.required.set(true)  // For human browsing
    }
}
```

### 6.4 `docker-compose.yml`

```yaml
services:
  db:
    image: postgres:16
    container_name: postgres
    environment:
      POSTGRES_USER: docker
      POSTGRES_PASSWORD: docker
      POSTGRES_DB: taskdb
    ports:
      - "5432:5432"
```

To start the database:
```bash
docker compose up -d
```

---

## 7. Complete Request Lifecycle — Step by Step

Let's trace a `POST /api/tasks` request with body:
```json
{
  "title": "<b>Buy Groceries</b>",
  "description": "Milk and eggs",
  "completed": false
}
```

**Step 1 — HTTP arrives at Tomcat**
Spring Boot's embedded Tomcat server receives the HTTP request.

**Step 2 — DispatcherServlet routes it**
Spring's `DispatcherServlet` (the "front controller") looks at the URL `/api/tasks` and method `POST`, and routes it to `TaskController.createTask()`.

**Step 3 — @RequestBody deserialization**
Jackson (the JSON library) reads the request body and creates a `TaskRequestDTO` object.

**Step 4 — @Valid validation**
Jakarta Bean Validation checks the constraints:
- `title` = `"<b>Buy Groceries</b>"` — NOT blank ✓, length OK ✓ (validation passes, sanitization comes later)
- `completed` = `false` — NOT null ✓

If validation fails here, `MethodArgumentNotValidException` is thrown → GlobalExceptionHandler returns 400.

**Step 5 — TaskController delegates to TaskService**
```java
return taskService.createTask(requestDto);
```

**Step 6 — SanitizationService strips HTML**
```
"<b>Buy Groceries</b>" → "Buy Groceries"
```
The `<b>` tag is harmless but AntiSamy strips all tags with the strict slashdot policy.

**Step 7 — Duplicate check**
```java
taskRepository.existsByHeaderAndCompletedFalse("Buy Groceries")
// SQL: SELECT COUNT(*) > 0 FROM tasks WHERE title = 'Buy Groceries' AND completed = false
// Result: false (no duplicate) — proceed
```

**Step 8 — MapStruct converts DTO → Entity**
```
TaskRequestDTO { title: "Buy Groceries", description: "Milk and eggs", completed: false }
    ↓  (TaskMapper.toEntity)
Task { id: null, header: "Buy Groceries", description: "Milk and eggs", completed: false }
```
Note: `title → header` (field name change), `id = null` (DB generates it).

**Step 9 — Hibernate saves to database**
```sql
INSERT INTO tasks (title, description, completed) VALUES ('Buy Groceries', 'Milk and eggs', false);
-- DB assigns id = 7 (for example)
```
`taskRepository.save()` returns the saved entity with `id = 7`.

**Step 10 — MapStruct converts Entity → ResponseDTO**
```
Task { id: 7, header: "Buy Groceries", description: "Milk and eggs", completed: false }
    ↓  (TaskMapper.toDTO)
TaskResponseDTO { id: 7, title: "Buy Groceries", description: "Milk and eggs",
                  completed: false, completionStatus: "PENDING" }
```
Note: `header → title`, and `completionStatus` is computed as `"PENDING"` (since `completed = false`).

**Step 11 — Jackson serializes to JSON**
```json
{
  "id": 7,
  "title": "Buy Groceries",
  "description": "Milk and eggs",
  "completed": false,
  "completionStatus": "PENDING"
}
```

**Step 12 — HTTP 200 OK response sent**
The controller returns this JSON with HTTP 200.

---

## 8. Spring Boot Key Concepts Explained

### Dependency Injection (DI)

Instead of writing:
```java
private TaskService taskService = new TaskService(new TaskRepository(), new TaskMapper(), ...);
```

You write:
```java
private final TaskService taskService;  // Spring injects this
```

Spring manages the creation and wiring of all objects. This is called **Inversion of Control (IoC)**.

### Spring Beans

Any class annotated with `@Component`, `@Service`, `@Repository`, or `@Controller` is a **Spring Bean** — Spring creates one instance and manages its lifecycle.

| Annotation | Used for |
|---|---|
| `@Component` | Generic component |
| `@Service` | Business logic layer |
| `@Repository` | Data access layer |
| `@RestController` | HTTP endpoint handler |

### Constructor Injection with `@RequiredArgsConstructor`

```java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;  // 'final' + no initialization
    private final TaskMapper taskMapper;
    // ...
}
```

Lombok's `@RequiredArgsConstructor` generates:
```java
public TaskService(TaskRepository taskRepository, TaskMapper taskMapper, ...) {
    this.taskRepository = taskRepository;
    this.taskMapper = taskMapper;
    // ...
}
```

Spring sees this constructor and automatically injects the matching beans. Constructor injection is preferred because it makes dependencies explicit and enables immutability.

### Optional and `orElseThrow`

```java
Optional<Task> optTask = taskRepository.findById(id);
// optTask might contain a Task, or it might be empty

Task task = optTask.orElseThrow(() -> new TaskNotFoundException(id));
// If present: unwrap and return the Task
// If empty:   throw TaskNotFoundException
```

`Optional` forces you to explicitly handle the "not found" case, preventing `NullPointerExceptions`.

### `@Transactional`

```java
@Transactional
public TaskResponseDTO createTask(TaskRequestDTO requestDto) {
    // All DB operations here are wrapped in one transaction
    // If ANYTHING throws an exception → all changes are rolled back
}
```

---

## 9. Testing Strategy

### 9.1 Test Types Used

| Test Class | Type | Spring Context? | What it tests |
|---|---|---|---|
| `TaskControllerTest` | Unit | ❌ No (Mockito + standalone MockMvc) | HTTP layer: routes, validation, error handling |
| `TaskServiceTest` | Unit | ❌ No (Mockito only) | Business logic: CRUD, duplicate check, 404 |
| `SanitizationServiceTest` | Unit | ❌ No (plain instantiation) | XSS sanitization logic |
| `GlobalExceptionHandlerTest` | Unit | ❌ No (plain instantiation) | Exception → HTTP status mapping |
| `TaskMapperTest` | Integration | ✅ Yes (@SpringBootTest, H2) | MapStruct field mappings |
| `TaskManagerApplicationTests` | Integration | ✅ Yes (non-web) | Application context starts successfully |

**Fast tests (no Spring context)** run in milliseconds. **Integration tests** take longer because they start a Spring context and connect to a database.

### 9.2 TaskControllerTest

Uses `@ExtendWith(MockitoExtension.class)` + standalone MockMvc. No Spring context needed.

```java
@ExtendWith(MockitoExtension.class)
class TaskControllerTest {
    private MockMvc mockMvc;

    @Mock
    private TaskService taskService;  // Fake TaskService

    @InjectMocks
    private TaskController taskController;  // Real controller with fake service

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }
}
```

**What `standaloneSetup` does:** Creates a minimal MockMvc that only loads the specified controller(s) — much faster than loading the full Spring context. The `GlobalExceptionHandler` is wired in explicitly.

**Example test:**
```java
@Test
void getAllTasks_ReturnsList() throws Exception {
    when(taskService.getAllTasks()).thenReturn(List.of(sampleResponse));

    mockMvc.perform(get("/api/tasks"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].title").value("Buy Groceries"));
}
```

Pattern: **Arrange** (mock behaviour) → **Act** (perform request) → **Assert** (check response).

### 9.3 TaskServiceTest

Uses only Mockito. Mocks all three collaborators.

```java
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock private TaskRepository taskRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private SanitizationService sanitizationService;

    @InjectMocks
    private TaskService taskService;
}
```

**Key patterns:**
```java
// When the repository is asked for task #1, return sampleTask:
when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

// Assert an exception is thrown:
assertThatThrownBy(() -> taskService.getTaskById(999L))
    .isInstanceOf(TaskNotFoundException.class);

// Verify the repository was actually called:
verify(taskRepository).findById(1L);
verify(taskRepository, never()).save(any());
```

### 9.4 SanitizationServiceTest

No mocking needed — tests the real AntiSamy sanitization:
```java
class SanitizationServiceTest {
    private SanitizationService sanitizationService;

    @BeforeEach
    void setUp() {
        sanitizationService = new SanitizationService(); // just new it up directly
    }

    @Test
    void stripsScriptTags() {
        String dirty = "<script>alert('xss')</script>Buy groceries";
        String clean = sanitizationService.sanitize(dirty);
        assertThat(clean).doesNotContain("<script>");
        assertThat(clean).doesNotContain("alert");
    }
}
```

### 9.5 GlobalExceptionHandlerTest

Tests the handler methods directly, without MockMvc:
```java
class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() { handler = new GlobalExceptionHandler(); }

    @Test
    void handleTaskNotFound_Returns404() {
        ResponseEntity<ErrorResponse> response =
            handler.handleTaskNotFound(new TaskNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
    }
}
```

### 9.6 TaskMapperTest

Uses `@SpringBootTest` with H2 to load the MapStruct-generated mapper bean:
```java
@SpringBootTest
class TaskMapperTest {

    @Autowired
    private TaskMapper taskMapper;   // Injects MapStruct-generated TaskMapperImpl

    @Test
    void mapsAllFieldsForIncompleteTask() {
        Task task = new Task(1L, "Buy Groceries", "Milk and eggs", false);
        TaskResponseDTO dto = taskMapper.toDTO(task);

        assertThat(dto.getTitle()).isEqualTo("Buy Groceries");    // header → title
        assertThat(dto.getCompletionStatus()).isEqualTo("PENDING"); // computed
    }
}
```

The test `application.yaml` in `src/test/resources/` uses H2, so no PostgreSQL server is needed.

### 9.7 TaskManagerApplicationTests

Verifies the application context starts without errors:
```java
class TaskManagerApplicationTests {
    @Test
    void mainStartsAndStops() {
        SpringApplication app = new SpringApplication(TaskManagerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // No web server — just context
        try (ConfigurableApplicationContext context = app.run()) {
            // If it reaches here, the context started successfully
        }
    }
}
```

`WebApplicationType.NONE` means no Tomcat server starts — just the Spring application context (with H2 database). This makes the test fast.

---

## 10. Code Coverage with JaCoCo

**JaCoCo** (Java Code Coverage) measures which lines of code are actually executed during your tests.

### Generating the Report

Run the tests (coverage is generated automatically after every test run):

```bash
./gradlew test
```

This is because of the Gradle configuration:
```kotlin
tasks.withType<Test> {
    finalizedBy(tasks.jacocoTestReport)  // Always run after tests
}
```

### Viewing the Report

**HTML report** (human-readable, open in browser):
```
build/reports/jacoco/test/html/index.html
```

Open this file in your browser to see a breakdown by package, class, and method.

**XML report** (for tools like SonarQube or CI/CD pipelines):
```
build/reports/jacoco/test/jacocoTestReport.xml
```

### Reading the HTML Report

The report shows:
- **Green** = covered by tests
- **Red** = NOT covered by tests
- **Yellow** = partially covered (e.g., one branch of an if-statement)

Metrics:
| Metric | Meaning |
|---|---|
| **Instructions** | Individual bytecode instructions executed |
| **Branches** | Both `true` and `false` paths of conditions covered |
| **Lines** | Lines that were executed |
| **Methods** | Methods that were called |
| **Classes** | Classes that were instantiated or used |

---

### Your Current Coverage (from `jacocoTestReport.xml`)

The existing JaCoCo report (generated from the last test run) shows the following numbers across the entire project:

| Metric | Covered | Missed | Total | Coverage % |
|---|---|---|---|---|
| **Instructions** | 38 | 420 | 458 | **~8.3%** |
| **Branches** | 1 | 19 | 20 | **~5%** |
| **Lines** | 13 | 112 | 125 | **~10.4%** |
| **Methods** | 5 | 27 | 32 | **~15.6%** |
| **Classes** | 4 | 5 | 9 | **~44%** |

#### Why is coverage so low when all tests pass?

This is a very important concept to understand. Your unit tests (using Mockito) **mock** out the real classes, so JaCoCo never sees your actual code being exercised:

```
TaskServiceTest:
  @Mock private TaskRepository taskRepository;  ← fake — real TaskService code NOT executed
  @Mock private TaskMapper taskMapper;           ← fake
  @InjectMocks private TaskService taskService;  ← real TaskService, but its collaborators are fakes
```

JaCoCo tracks which **real bytecode** runs. When `taskRepository.findById(1L)` is called on a Mockito mock, the real `TaskRepository` and real `TaskService` internal logic are still invoked — BUT the service methods themselves **are** exercised. However, since the last test run only ran `TaskManagerApplicationTests` (which boots the context with no web and does no CRUD operations), almost none of the real application code was actually called.

#### Per-package breakdown

| Package | Instructions Covered | Instructions Missed | Coverage |
|---|---|---|---|
| `com.taskmanager.app` (main class) | 3 | 5 | ~37% |
| `com.taskmanager.app.controller` | 0 | 27 | **0%** |
| `com.taskmanager.app.service` | 29 | 204 | ~12% |
| `com.taskmanager.app.mapper` | 3 | 60 | ~5% |
| `com.taskmanager.app.exception` | 3 | 124 | ~2% |
| `com.taskmanager.app.repository` | 0 | 0 | N/A (interface) |
| `com.taskmanager.app.model` | 0 | 0 | N/A (Lombok generated) |
| `com.taskmanager.app.dto` | 0 | 0 | N/A (Lombok generated) |

#### How to get accurate coverage from Mockito-based unit tests

Your unit tests DO test the real service/controller logic, but since they use Mockito mocks the JaCoCo numbers appear misleadingly low. The reason is that your tests are correct — they isolate the unit under test. To make JaCoCo reflect this properly, you need to ensure the test classes themselves exercise the real code paths.

**Your unit tests are testing correctly.** The low JaCoCo numbers are largely because the `TaskManagerApplicationTests` boots the context but makes no API calls — it's just a smoke test that the context loads. Meanwhile, the other tests (using Mockito) DO call the real methods but only on Mockito-managed objects.

To improve JaCoCo numbers, you could add integration tests that make real HTTP calls through MockMvc with `@SpringBootTest` — those would exercise the real service, mapper, and repository code together.

---

### Understanding Coverage Numbers

**100% coverage does NOT mean your code is bug-free.** It only means every line was executed at least once. You still need meaningful assertions.

**Typical coverage targets:**
- Service layer: aim for 80–100%
- Controller layer: aim for 80–100%
- Exception handling: aim for 90–100%
- Mapper: aim for 100% (it's generated code, but verify the mappings)

---

## 11. API Reference

Base URL: `http://localhost:9090`

### GET /api/tasks
Returns all tasks.

**Response:**
```json
[
  {
    "id": 1,
    "title": "Buy Groceries",
    "description": "Milk and eggs",
    "completed": false,
    "completionStatus": "PENDING"
  }
]
```

### GET /api/tasks/{id}
Returns a single task.

**Response:** Same structure as one element above.

**Error (not found):**
```json
{ "status": 404, "error": "Not Found", "message": "Task with ID 99 not found", "timestamp": "..." }
```

### POST /api/tasks
Creates a new task.

**Request body:**
```json
{
  "title": "Buy Groceries",
  "description": "Milk and eggs",
  "completed": false
}
```

**Response:** `200 OK` with the created task (includes DB-generated `id`).

**Validation error:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "errors": { "title": "Title is required" },
  "timestamp": "..."
}
```

### PUT /api/tasks/{id}
Updates an existing task.

**Request body:** Same as POST.

**Response:** `200 OK` with the updated task.

### DELETE /api/tasks/{id}
Deletes a task.

**Response:** `204 No Content` (empty body, success).

---

## 12. Key Design Decisions Explained

### Why is the entity field named `header` but the DB column is `title`?

This is a deliberate teaching example showing that:
1. The Java model (`header`) and database column (`title`) can have different names using `@Column(name = "title")`.
2. The DTO field (`title`) can also differ from the entity field (`header`).
3. The mapper (`@Mapping(source = "header", target = "title")`) bridges the gap.

In practice, you'd usually keep names consistent, but this demonstrates that Spring + MapStruct handle the translation cleanly.

### Why sanitize input in the Service, not the DTO or Controller?

- **DTOs** are plain data holders. Business logic doesn't belong there.
- **Controllers** handle HTTP concerns (routing, validation). Security is a business concern.
- **Services** are the right place because: all writes go through the service; if you add another entry point (e.g., a message queue consumer), sanitization is already there.

### Why use `@Transactional` on write operations?

If `createTask` ran the duplicate check (read), then the insert (write), and the insert failed for some reason, you want the whole operation to be atomic. `@Transactional` ensures this. Without it, you could have partial state.

### Why Boolean (not boolean) for the `completed` field in RequestDTO?

With `boolean`, the JVM initializes it to `false` even if the client omits the field from JSON. That means:
- Client sends `{ "title": "..." }` (no `completed` field)
- Java sees `completed = false` — silently wrong
- `@NotNull` has no effect on primitives

With `Boolean` (wrapper):
- Client sends `{ "title": "..." }` (no `completed` field)
- Java sees `completed = null`
- `@NotNull` triggers a 400 error — correct behavior

### Why not use `@SpringBootTest` for all tests?

`@SpringBootTest` starts a full Spring application context — which takes seconds. Mockito-based unit tests run in milliseconds. For a project with hundreds of tests, this time difference is significant. Use `@SpringBootTest` only when you need Spring to wire things together (e.g., testing MapStruct-generated code or integration flows).

---

## 13. Common Errors and How They Are Handled

| Scenario | What happens | HTTP response |
|---|---|---|
| `GET /api/tasks/999` (ID doesn't exist) | `TaskNotFoundException` thrown → caught by handler | `404 Not Found` |
| `POST /api/tasks` with empty title | `@Valid` triggers `MethodArgumentNotValidException` | `400 Bad Request` |
| `POST /api/tasks` with duplicate active title | `DuplicateTaskException` thrown | `409 Conflict` |
| `POST /api/tasks` without `completed` field | `@Valid` → `@NotNull` fails | `400 Bad Request` |
| `POST /api/tasks` with `<script>` in title | AntiSamy strips the tag before save | Script is removed, `200 OK` |
| `POST /api/tasks` with title too long (>100 chars) | `@Size` triggers validation error | `400 Bad Request` |
| Any unexpected error | Caught by the generic `Exception` handler | `500 Internal Server Error` (no internal details exposed) |

---

*This document was generated on February 24, 2026, covering Spring Boot 4.0.3, Java 25.*

