# Deep Dive: Global Exception Handler in Spring Boot

## The Problem It Solves

Without a global exception handler, if you call:
```bash
curl http://localhost:9090/api/tasks/999
```
Spring returns a generic **500 Internal Server Error** with a noisy HTML/JSON stack trace. That's bad — it leaks internal implementation details and gives a poor API experience.

With `GlobalExceptionHandler`, you get a clean:
```
HTTP 404 Not Found
"Task with ID 999 not found"
```

---

## The Flow — Step by Step

```
HTTP Request
     │
     ▼
┌─────────────────┐
│  TaskController  │  getTaskById(999) ──────────────────────────────────┐
└─────────────────┘                                                       │
                                                                          ▼
                                                           ┌──────────────────────┐
                                                           │     TaskService      │
                                                           │  taskRepository      │
                                                           │  .findById(999)      │
                                                           │  .orElseThrow(       │
                                                           │    RuntimeException) │ ← Exception THROWN here
                                                           └──────────────────────┘
                                                                          │
                                                    Exception bubbles UP the call stack
                                                                          │
                                                                          ▼
                                                     ┌──────────────────────────────────┐
                                                     │  Spring's DispatcherServlet      │
                                                     │  catches the unhandled exception │
                                                     │  and looks for a handler...      │
                                                     └──────────────────────────────────┘
                                                                          │
                                                                          ▼
                                                     ┌──────────────────────────────────┐
                                                     │    GlobalExceptionHandler         │
                                                     │  @RestControllerAdvice            │
                                                     │  handleTaskNotFound(ex) called    │
                                                     │  → returns 404 + message          │
                                                     └──────────────────────────────────┘
                                                                          │
                                                                          ▼
                                                               HTTP Response sent
                                                               to the client
```

---

## Annotation Deep Dive

### `@RestControllerAdvice`

This is a **combination of two annotations**:

```java
@ControllerAdvice   // Intercepts exceptions from ALL controllers globally
+
@ResponseBody       // Serializes the return value to JSON (not a view/HTML)
= @RestControllerAdvice
```

- It acts like a **"safety net"** that wraps all your controllers.
- Spring automatically scans for it at startup and registers it as an exception interceptor.
- You only need **one** in the whole application.

### `@ExceptionHandler(SomeException.class)`

```java
@ExceptionHandler(TaskNotFoundException.class)
public ResponseEntity<String> handleTaskNotFound(TaskNotFoundException ex) { ... }
```

- Tells Spring: *"If a `TaskNotFoundException` is thrown from any controller, call THIS method."*
- The `ex` parameter is automatically injected by Spring — it's the actual exception that was thrown.
- You can have **multiple** `@ExceptionHandler` methods for different exception types.

---

## Your Code Traced

```java
// TaskService.java
public Task getTaskById(Long id) {
    return taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
    //                         ▲
    //                         │ This exception is thrown when ID doesn't exist in DB
}
```

```java
// GlobalExceptionHandler.java
@ExceptionHandler(TaskNotFoundException.class)
public ResponseEntity<String> handleTaskNotFound(TaskNotFoundException ex) {
    //                                                      ▲
    //                    Spring injects the exception here, ex.getMessage()
    //                    returns → "Task with ID 999 not found"

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    //                   ▲                                   ▲
    //                   Sets HTTP status = 404              The message from the exception
}
```

---

## Why NOT Use `RuntimeException` as the Handler

`RuntimeException` is the **base class** for ALL runtime exceptions. Catching it is **too broad**:

```
RuntimeException
├── NullPointerException      ← should be 500 Internal Server Error
├── IllegalArgumentException  ← should be 400 Bad Request
├── ArithmeticException       ← should be 500 Internal Server Error
└── TaskNotFoundException     ← ✅ should be 404 Not Found
```

---

## The Professional Approach: Custom Exceptions (Your Current Code)

### Step 1 — Custom exception class

```java
// exception/TaskNotFoundException.java
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) {
        super("Task with ID " + id + " not found");
    }
}
```

### Step 2 — Throw it from the Service

```java
// TaskService.java
public Task getTaskById(Long id) {
    return taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
}
```

### Step 3 — Handle specifically in GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles only "task not found" → 404
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<String> handleTaskNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // Handles bad input → 400
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // Catch-all for anything unexpected → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body("Something went wrong. Please try again later.");
        // ↑ Never expose internal details to the client!
    }
}
```

---

## Even Better: Return a Structured Error Body

Instead of returning a plain `String`, return a proper JSON error object:

```java
// exception/ErrorResponse.java
public class ErrorResponse {
    private int status;
    private String message;
    private String timestamp;
    // constructor, getters...
}

// In GlobalExceptionHandler:
@ExceptionHandler(TaskNotFoundException.class)
public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
        404,
        ex.getMessage(),
        LocalDateTime.now().toString()
    );
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}
```

Client gets a structured JSON:
```json
{
  "status": 404,
  "message": "Task with ID 999 not found",
  "timestamp": "2026-02-22T10:30:00"
}
```

---

## Exception Matching: Most Specific Wins

Spring always matches the **most specific** exception handler:

```
Exception thrown: TaskNotFoundException

Candidates:
  @ExceptionHandler(TaskNotFoundException.class) ← ✅ MATCHED (most specific)
  @ExceptionHandler(RuntimeException.class)      ← skipped
  @ExceptionHandler(Exception.class)             ← skipped (catch-all, least specific)
```

---

## Summary

| Concept | What it does |
|---|---|
| `@RestControllerAdvice` | Marks the class as a global exception interceptor for all controllers |
| `@ExceptionHandler(X.class)` | Registers a method to handle exception type `X` |
| `ResponseEntity<T>` | Lets you control both the HTTP status code and the response body |
| Custom exceptions | Gives you precise, meaningful exception handling instead of catching everything |
| Exception hierarchy | More specific handlers are matched first; Spring picks the closest match |

**Key Insight:** Spring's `DispatcherServlet` is the middleman — it catches all unhandled exceptions
from controllers and delegates them to whichever `@ExceptionHandler` method matches best.

