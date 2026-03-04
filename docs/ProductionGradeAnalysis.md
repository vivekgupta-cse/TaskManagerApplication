# Production-Grade Analysis — TaskManagerApplication

> What would it take to turn this learning project into a **truly production-ready, scalable, secure microservice**?
>
> This document analyses every gap and provides concrete recommendations with code examples.
>
> Last updated: 2026-03-04

---

## Table of Contents

1. [Current State Assessment](#1-current-state-assessment)
2. [Security — Critical Gaps](#2-security--critical-gaps)
3. [Observability — Logging, Metrics, Tracing](#3-observability--logging-metrics-tracing)
4. [Resilience & Reliability](#4-resilience--reliability)
5. [Scalability](#5-scalability)
6. [API Design Improvements](#6-api-design-improvements)
7. [Database & Data Layer](#7-database--data-layer)
8. [Configuration & Environment Management](#8-configuration--environment-management)
9. [CI/CD & Deployment](#9-cicd--deployment)
10. [Testing Improvements](#10-testing-improvements)
11. [Documentation & API Contract](#11-documentation--api-contract)
12. [Priority Roadmap](#12-priority-roadmap)

---

## 1. Current State Assessment

### ✅ What's Already Good

| Aspect | What's in Place |
|---|---|
| Layered architecture | Controller → Service → Repository separation |
| DTO pattern | Separate request/response DTOs prevent entity leaking |
| Input validation | Jakarta Validation (`@NotBlank`, `@Size`, `@NotNull`) |
| XSS prevention | AntiSamy sanitization in the service layer |
| Soft delete | `@SQLDelete` + `@SQLRestriction` |
| Schema management | Flyway versioned migrations |
| JPA auditing | `@CreatedDate`, `@LastModifiedDate` |
| Pagination | `Pageable` with `@PageableDefault` |
| Dynamic filtering | JPA Specifications |
| Global error handling | `@RestControllerAdvice` with consistent `ErrorResponse` |
| Test coverage | Unit tests (Mockito), integration tests (real Postgres) |
| Separate test DB | `docker-compose-test.yml` on port 5433 |

### ❌ What's Missing for Production

| Gap | Risk Level | Section |
|---|---|---|
| No authentication/authorisation | 🔴 Critical | §2.1 |
| No HTTPS / TLS | 🔴 Critical | §2.2 |
| No rate limiting | 🔴 Critical | §2.3 |
| No CORS configuration | 🟡 High | §2.4 |
| Credentials in plain text | 🔴 Critical | §8.1 |
| No health checks / readiness probes | 🟡 High | §3.1 |
| No metrics (Prometheus/Grafana) | 🟡 High | §3.2 |
| No distributed tracing | 🟡 High | §3.3 |
| No structured logging (JSON) | 🟡 High | §3.4 |
| No request/response logging | 🟡 High | §3.5 |
| No caching | 🟠 Medium | §5.1 |
| No async processing | 🟠 Medium | §5.2 |
| No API versioning | 🟠 Medium | §6.1 |
| No HATEOAS | 🟢 Low | §6.2 |
| No Dockerfile for the app | 🟡 High | §9.1 |
| No CI/CD pipeline | 🟡 High | §9.2 |
| No integration tests (end-to-end) | 🟠 Medium | §10.1 |
| No OpenAPI/Swagger docs | 🟠 Medium | §11.1 |

---

## 2. Security — Critical Gaps

### 2.1 Authentication & Authorisation

**Current state:** The API is completely open — anyone can create, read, update, or delete any task.

**Recommendation:** Add Spring Security with JWT (JSON Web Tokens).

```
Add to build.gradle.kts:
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
```

**Implementation plan:**

```
┌─────────────┐     ┌──────────────┐     ┌───────────────────┐
│  Client      │────▶│ API Gateway  │────▶│ TaskManager API   │
│ (with JWT)   │     │ (validates)  │     │ (checks roles)    │
└─────────────┘     └──────────────┘     └───────────────────┘
```

1. **Add a `SecurityConfig` class** — configure HTTP security filters
2. **Use `@PreAuthorize`** annotations on controller methods:
   ```java
   @PreAuthorize("hasRole('USER')")
   @GetMapping
   public ResponseEntity<Page<TaskResponseDTO>> getAllTasks(...) { ... }

   @PreAuthorize("hasRole('ADMIN')")
   @DeleteMapping("/{id}")
   public ResponseEntity<Void> deleteTask(...) { ... }
   ```
3. **Add a `User` entity** with roles (or integrate with an external Identity Provider like Keycloak/Auth0)
4. **Multi-tenancy**: Each user should only see their own tasks. Add a `userId` column to the `tasks` table and filter by the authenticated user.

### 2.2 HTTPS / TLS

**Current state:** The application runs on plain HTTP (port 9090).

**Recommendation:**
- **Option A (recommended):** Terminate TLS at a reverse proxy (Nginx, Traefik, AWS ALB). The app remains HTTP internally.
- **Option B:** Configure TLS in Spring Boot directly (for development/testing):

```yaml
server:
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    enabled: true
  port: 8443
```

### 2.3 Rate Limiting

**Current state:** No rate limiting. A single client can flood the API with unlimited requests.

**Recommendation:** Add rate limiting at the API Gateway level (preferred) or application level.

**Application-level option using Bucket4j:**
```
Add to build.gradle.kts:
  implementation("com.bucket4j:bucket4j_jcache-jakarta:8.12.1")
```

Create a rate-limiting filter:
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    // 100 requests per minute per IP
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) {
        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> createBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("Rate limit exceeded. Try again later.");
        }
    }
}
```

### 2.4 CORS Configuration

**Current state:** No CORS configuration. Browsers will block cross-origin requests from frontend apps.

**Recommendation:** Add a CORS configuration bean:

```java
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("https://your-frontend.com")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
```

**Never use `allowedOrigins("*")` in production** — it disables CORS protection entirely.

### 2.5 Additional Security Hardening

| Item | Recommendation |
|---|---|
| **Security headers** | Add `Content-Security-Policy`, `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security` headers via a filter or Spring Security |
| **SQL injection** | Already mitigated by JPA parameterised queries, but add input length limits at the network level (WAF) |
| **Dependency scanning** | Add OWASP Dependency-Check Gradle plugin to scan for known CVEs in dependencies |
| **Secrets management** | Use HashiCorp Vault, AWS Secrets Manager, or Kubernetes Secrets instead of plain-text credentials |
| **Audit logging** | Log WHO did WHAT and WHEN. Add a `modified_by` column to the tasks table |

---

## 3. Observability — Logging, Metrics, Tracing

### 3.1 Health Checks & Readiness Probes

**Current state:** No health endpoints. Kubernetes/load balancers can't determine if the app is healthy.

**Recommendation:** Add Spring Boot Actuator:

```
Add to build.gradle.kts:
  implementation("org.springframework.boot:spring-boot-starter-actuator")
```

```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true      # Enables /actuator/health/liveness and /actuator/health/readiness
  health:
    db:
      enabled: true         # Checks PostgreSQL connectivity
```

This provides:
- `GET /actuator/health` — overall health status
- `GET /actuator/health/liveness` — Kubernetes liveness probe
- `GET /actuator/health/readiness` — Kubernetes readiness probe (checks DB, Flyway, etc.)

### 3.2 Metrics (Prometheus + Grafana)

**Current state:** No metrics collection. You have zero visibility into request rates, error rates, latency percentiles, DB connection pool usage, etc.

**Recommendation:**

```
Add to build.gradle.kts:
  implementation("io.micrometer:micrometer-registry-prometheus")
```

This auto-exposes `/actuator/prometheus` with metrics like:
- `http_server_requests_seconds` — request latency histograms
- `hikaricp_connections_active` — DB connection pool usage
- `jvm_memory_used_bytes` — JVM memory usage
- `jvm_threads_live_threads` — Virtual thread count

**Custom business metrics:**
```java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final MeterRegistry meterRegistry;

    public TaskResponseDTO createTask(TaskRequestDTO requestDto) {
        // ... existing logic ...
        meterRegistry.counter("tasks.created").increment();
        meterRegistry.counter("tasks.created.by_status",
                "completed", String.valueOf(requestDto.getCompleted())).increment();
        return taskMapper.toDTO(savedTask);
    }
}
```

### 3.3 Distributed Tracing

**Current state:** No tracing. When a request fails, you can't trace it across services.

**Recommendation:** Add Micrometer Tracing (formerly Spring Cloud Sleuth):

```
Add to build.gradle.kts:
  implementation("io.micrometer:micrometer-tracing-bridge-otel")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")
```

This auto-injects trace IDs into every log line and HTTP header, enabling end-to-end request tracing in tools like Jaeger, Zipkin, or Grafana Tempo.

### 3.4 Structured Logging (JSON)

**Current state:** Human-readable log lines. Hard to parse, search, and aggregate in production log systems (ELK, Datadog, CloudWatch).

**Recommendation:** Switch to structured JSON logging:

```
Add to build.gradle.kts:
  implementation("net.logstash.logback:logstash-logback-encoder:8.0")
```

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

Output:
```json
{
  "timestamp": "2026-03-04T10:30:00.123Z",
  "level": "INFO",
  "logger": "com.taskmanager.app.service.TaskService",
  "message": "Task created successfully",
  "traceId": "abc123def456",
  "spanId": "789ghi",
  "taskId": 42
}
```

### 3.5 Request/Response Logging

**Current state:** No HTTP request/response logging. You can't see what was sent to the API when investigating issues.

**Recommendation:** Add a logging filter:

```java
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws Exception {
        long start = System.currentTimeMillis();
        filterChain.doFilter(request, response);
        long duration = System.currentTimeMillis() - start;

        log.info("method={} uri={} status={} duration={}ms",
                request.getMethod(), request.getRequestURI(),
                response.getStatus(), duration);
    }
}
```

---

## 4. Resilience & Reliability

### 4.1 Database Connection Pooling

**Current state:** Uses HikariCP (Spring Boot default) but with default settings.

**Recommendation:** Tune the connection pool for production:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20           # Max concurrent DB connections
      minimum-idle: 5                 # Min idle connections kept alive
      connection-timeout: 30000       # 30s — fail fast if DB is overloaded
      idle-timeout: 600000            # 10min — close idle connections
      max-lifetime: 1800000           # 30min — recycle connections before DB kills them
      leak-detection-threshold: 60000 # Log if connection is held for > 60s
```

### 4.2 Retry & Circuit Breaker

**Current state:** No retry logic. A transient DB failure immediately returns 500 to the client.

**Recommendation:** Add Spring Retry + Resilience4j:

```
Add to build.gradle.kts:
  implementation("org.springframework.retry:spring-retry")
  implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
```

```java
@Service
public class TaskService {

    @Retryable(retryFor = DataAccessException.class, maxAttempts = 3,
               backoff = @Backoff(delay = 100, multiplier = 2))
    public TaskResponseDTO createTask(TaskRequestDTO requestDto) { ... }
}
```

### 4.3 Graceful Shutdown

**Current state:** No graceful shutdown configuration. Killing the app mid-request causes data corruption.

```yaml
server:
  shutdown: graceful               # Wait for in-flight requests to complete

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # Max wait time before force-killing
```

### 4.4 Idempotency

**Current state:** `POST /api/tasks` is not idempotent. If the client retries a failed request, it may create duplicate tasks.

**Recommendation:** Add an `Idempotency-Key` header:

```java
@PostMapping
public TaskResponseDTO createTask(
        @Valid @RequestBody TaskRequestDTO requestDto,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

    if (idempotencyKey != null) {
        // Check if we've already processed this key (use Redis or a DB table)
        Optional<TaskResponseDTO> cached = idempotencyCache.get(idempotencyKey);
        if (cached.isPresent()) return cached.get();
    }
    TaskResponseDTO result = taskService.createTask(requestDto);
    if (idempotencyKey != null) {
        idempotencyCache.put(idempotencyKey, result);
    }
    return result;
}
```

---

## 5. Scalability

### 5.1 Caching

**Current state:** Every `GET /api/tasks/{id}` hits the database, even for frequently-accessed tasks.

**Recommendation:** Add Spring Cache with Redis:

```
Add to build.gradle.kts:
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-cache")
```

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    @Cacheable(value = "tasks", key = "#id")
    public TaskResponseDTO getTaskById(Long id) { ... }

    @CacheEvict(value = "tasks", key = "#id")
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDto) { ... }

    @CacheEvict(value = "tasks", key = "#id")
    public void deleteTask(Long id) { ... }

    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponseDTO createTask(TaskRequestDTO requestDto) { ... }
}
```

### 5.2 Async / Event-Driven Processing

**Current state:** All operations are synchronous. Heavy operations (like sanitization on bulk imports) block the request thread.

**Recommendation:** Use Spring Events for side effects:

```java
// Publish event after creation
applicationEventPublisher.publishEvent(new TaskCreatedEvent(savedTask));

// Listener handles side effects asynchronously
@Async
@EventListener
public void handleTaskCreated(TaskCreatedEvent event) {
    // Send notification, update search index, audit log, etc.
}
```

### 5.3 Database Read Replicas

**Current state:** Single database instance. All reads and writes go to the same PostgreSQL server.

**Recommendation:** For high-read workloads, add read replicas:

```java
@Configuration
public class DataSourceConfig {

    @Bean @Primary
    @ConfigurationProperties("spring.datasource.write")
    public DataSource writeDataSource() { ... }

    @Bean
    @ConfigurationProperties("spring.datasource.read")
    public DataSource readDataSource() { ... }
}
```

Route read-only transactions to the replica:
```java
@Transactional(readOnly = true)
public Page<TaskResponseDTO> getAllTasks(...) { ... }
```

### 5.4 Containerisation & Horizontal Scaling

**Current state:** No Dockerfile for the application. Can't be deployed to Kubernetes.

**Recommendation:** Add a multi-stage Dockerfile:

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime (smaller image)
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Non-root user for security
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 6. API Design Improvements

### 6.1 API Versioning

**Current state:** No API versioning. Breaking changes will break all clients simultaneously.

**Recommendation:** URL-based versioning (simplest):

```java
@RestController
@RequestMapping("/api/v1/tasks")   // ← versioned URL
public class TaskController { ... }
```

When you need breaking changes, create `/api/v2/tasks` while keeping v1 active.

### 6.2 Proper HTTP Status Codes

**Current state:** `POST /api/tasks` returns `200 OK`. The correct status is `201 Created`.

**Recommendation:**

```java
@PostMapping
public ResponseEntity<TaskResponseDTO> createTask(@Valid @RequestBody TaskRequestDTO requestDto) {
    TaskResponseDTO created = taskService.createTask(requestDto);
    URI location = URI.create("/api/v1/tasks/" + created.getId());
    return ResponseEntity.created(location).body(created);  // 201 Created + Location header
}
```

### 6.3 `PATCH` for Partial Updates

**Current state:** Only `PUT` is supported — clients must send ALL fields even to change one.

**Recommendation:** Add `PATCH` endpoint:

```java
@PatchMapping("/{id}")
public TaskResponseDTO patchTask(@PathVariable Long id,
                                  @RequestBody Map<String, Object> updates) {
    return taskService.patchTask(id, updates);
}
```

### 6.4 Response Envelope Consistency

**Current state:** Success responses return the DTO directly; error responses use `ErrorResponse`. Inconsistent.

**Recommendation:** Wrap all responses:

```json
{
  "success": true,
  "data": { "id": 1, "title": "Buy Groceries", ... },
  "error": null,
  "timestamp": "2026-03-04T10:30:00"
}
```

---

## 7. Database & Data Layer

### 7.1 Unique Constraint with Soft Delete

**Current state:** The `UNIQUE(title)` constraint blocks creating a new task with the same title as a soft-deleted task. A user deletes "Buy Groceries" but can never create a task with that name again.

**Recommendation:** Use a **partial unique index** (PostgreSQL-specific):

```sql
-- V4__fix_unique_constraint_for_soft_delete.sql
ALTER TABLE tasks DROP CONSTRAINT uc_task_title;
CREATE UNIQUE INDEX uix_tasks_title_active ON tasks (title) WHERE deleted = false;
```

This allows multiple soft-deleted rows with the same title, but only one active row per title.

### 7.2 Database Indexing

**Current state:** Only `idx_tasks_deleted` index exists.

**Recommendation:** Add indexes for common query patterns:

```sql
-- V5__add_performance_indexes.sql
CREATE INDEX idx_tasks_completed ON tasks(completed) WHERE deleted = false;
CREATE INDEX idx_tasks_title_lower ON tasks(LOWER(title)) WHERE deleted = false;
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC) WHERE deleted = false;
```

### 7.3 Connection Pool Monitoring

Add HikariCP metrics to Prometheus:

```yaml
management:
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true
```

### 7.4 Database Backup & Recovery

**Current state:** No backup strategy. If the Docker volume is lost, all data is gone.

**Recommendation:**
- Mount PostgreSQL data to a persistent volume
- Set up automated `pg_dump` backups (daily)
- Test recovery procedures quarterly
- Consider managed PostgreSQL (AWS RDS, Cloud SQL, Azure Database) for production

---

## 8. Configuration & Environment Management

### 8.1 Secrets Management

**Current state:** Database credentials are hardcoded:

```yaml
username: ${DB_USERNAME:docker}   # Default "docker" is in plain text
password: ${DB_PASSWORD:docker}   # NEVER in production
```

**Recommendation:**

| Environment | Strategy |
|---|---|
| **Local dev** | `.env` file (git-ignored) + Docker Compose `env_file:` |
| **CI/CD** | GitHub Actions secrets / GitLab CI variables |
| **Kubernetes** | Kubernetes Secrets mounted as environment variables |
| **Production** | HashiCorp Vault / AWS Secrets Manager with auto-rotation |

### 8.2 Profile-Based Configuration

**Current state:** Single `application.yaml` for all environments.

**Recommendation:** Split into profiles:

```
src/main/resources/
├── application.yaml              ← Common settings
├── application-dev.yaml          ← Local development (docker credentials)
├── application-staging.yaml      ← Staging environment
└── application-prod.yaml         ← Production (strict settings)
```

```yaml
# application-prod.yaml
spring:
  jpa.hibernate.ddl-auto: none
  flyway.clean-disabled: true     # Prevent accidental schema wipe
  datasource:
    hikari.maximum-pool-size: 30

logging:
  level:
    root: WARN
    org.hibernate.SQL: WARN       # No SQL logging in production
```

Activate with: `SPRING_PROFILES_ACTIVE=prod`

---

## 9. CI/CD & Deployment

### 9.1 Dockerfile

See [Section 5.4](#54-containerisation--horizontal-scaling) above.

### 9.2 CI/CD Pipeline (GitHub Actions Example)

```yaml
# .github/workflows/ci.yml
name: CI

on: [push, pull_request]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: docker
          POSTGRES_PASSWORD: docker
          POSTGRES_DB: taskdb
        ports:
          - 5433:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 25
      - name: Build & Test
        run: ./gradlew build
      - name: Upload Test Report
        uses: actions/upload-artifact@v4
        with:
          name: test-report
          path: build/reports/
      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          file: build/reports/jacoco/test/jacocoTestReport.xml
```

### 9.3 Kubernetes Deployment

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: task-manager
spec:
  replicas: 3
  selector:
    matchLabels:
      app: task-manager
  template:
    spec:
      containers:
        - name: task-manager
          image: your-registry/task-manager:latest
          ports:
            - containerPort: 9090
          env:
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: username
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 9090
            initialDelaySeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 9090
            initialDelaySeconds: 10
          resources:
            requests:
              cpu: 250m
              memory: 512Mi
            limits:
              cpu: 1000m
              memory: 1Gi
```

---

## 10. Testing Improvements

### 10.1 End-to-End Integration Tests

**Current state:** Controller tests use standalone MockMvc (no real HTTP). Service tests mock the repository.

**Recommendation:** Add true end-to-end tests that hit the real API over HTTP with a real database:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanDb() {
        taskRepository.deleteAll();  // Hard delete for test cleanup
    }

    @Test
    void createAndRetrieveTask() {
        // POST
        TaskRequestDTO request = TaskRequestDTO.builder()
                .title("Integration Test Task").completed(false).build();
        ResponseEntity<TaskResponseDTO> createResponse =
                restTemplate.postForEntity("/api/tasks", request, TaskResponseDTO.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long id = createResponse.getBody().getId();

        // GET
        ResponseEntity<TaskResponseDTO> getResponse =
                restTemplate.getForEntity("/api/tasks/" + id, TaskResponseDTO.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getTitle()).isEqualTo("Integration Test Task");
    }
}
```

### 10.2 Contract Testing

For microservice-to-microservice communication, add Spring Cloud Contract or Pact to ensure API contracts are not broken.

### 10.3 Performance / Load Testing

Add Gatling or k6 scripts to test API throughput under load:

```
# k6 load test example
k6 run --vus 100 --duration 30s load-test.js
```

### 10.4 Mutation Testing

Add PIT (Pitest) to verify that tests actually catch bugs, not just execute code:

```
Add to build.gradle.kts:
  id("info.solidsoft.pitest") version "1.15.0"
```

---

## 11. Documentation & API Contract

### 11.1 OpenAPI / Swagger

**Current state:** No API documentation. Clients must read the source code to understand endpoints.

**Recommendation:**

```
Add to build.gradle.kts:
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
```

This auto-generates:
- `GET /swagger-ui.html` — interactive API explorer
- `GET /v3/api-docs` — OpenAPI 3.0 JSON spec

Enhance with annotations:

```java
@Operation(summary = "Create a new task",
           description = "Creates a task after sanitising input and checking for duplicates")
@ApiResponse(responseCode = "200", description = "Task created successfully")
@ApiResponse(responseCode = "400", description = "Validation failed")
@ApiResponse(responseCode = "409", description = "Active task with same title already exists")
@PostMapping
public TaskResponseDTO createTask(@Valid @RequestBody TaskRequestDTO requestDto) { ... }
```

### 11.2 API Changelog

Maintain a `CHANGELOG.md` documenting every API change, deprecation, and breaking change.

---

## 12. Priority Roadmap

### Phase 1 — Security & Observability (Week 1–2)

| # | Task | Priority |
|---|---|---|
| 1 | Add Spring Boot Actuator (health checks) | 🔴 Critical |
| 2 | Add Spring Security + JWT authentication | 🔴 Critical |
| 3 | Move credentials to environment variables / secrets | 🔴 Critical |
| 4 | Add CORS configuration | 🔴 Critical |
| 5 | Add rate limiting | 🔴 Critical |
| 6 | Add structured JSON logging | 🟡 High |
| 7 | Add Prometheus metrics | 🟡 High |

### Phase 2 — Reliability & Deployment (Week 3–4)

| # | Task | Priority |
|---|---|---|
| 8 | Create Dockerfile (multi-stage) | 🟡 High |
| 9 | Set up CI/CD pipeline (GitHub Actions) | 🟡 High |
| 10 | Add graceful shutdown | 🟡 High |
| 11 | Fix partial unique index for soft delete | 🟡 High |
| 12 | Add profile-based configuration | 🟡 High |
| 13 | Add distributed tracing | 🟡 High |

### Phase 3 — Scalability & Polish (Week 5–6)

| # | Task | Priority |
|---|---|---|
| 14 | Add Redis caching | 🟠 Medium |
| 15 | Add OpenAPI/Swagger docs | 🟠 Medium |
| 16 | Add end-to-end integration tests | 🟠 Medium |
| 17 | Add API versioning (`/api/v1/`) | 🟠 Medium |
| 18 | Fix `POST` to return `201 Created` | 🟠 Medium |
| 19 | Add `PATCH` endpoint for partial updates | 🟠 Medium |
| 20 | Add database performance indexes | 🟠 Medium |

### Phase 4 — Advanced (Week 7+)

| # | Task | Priority |
|---|---|---|
| 21 | Add retry/circuit breaker (Resilience4j) | 🟠 Medium |
| 22 | Add idempotency keys | 🟠 Medium |
| 23 | Add event-driven processing | 🟢 Low |
| 24 | Add read replicas configuration | 🟢 Low |
| 25 | Add mutation testing (Pitest) | 🟢 Low |
| 26 | Add contract testing | 🟢 Low |
| 27 | Kubernetes manifests | 🟢 Low |

---

*This analysis is based on the actual codebase of TaskManagerApplication as of 2026-03-04 and reflects industry best practices for Spring Boot microservices in production.*

