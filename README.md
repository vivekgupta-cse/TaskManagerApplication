# Task Manager Application
A production-grade Spring Boot REST API for task management with JWT authentication, PostgreSQL database, and comprehensive testing.


#Plan
![[Design_Task_Manager.excalidraw.md]]

## Features
- ✅ RESTful API for task CRUD operations
- 🔐 JWT-based authentication
- 🐘 PostgreSQL database with Flyway migrations
- 🧪 Comprehensive test suite (Unit + Integration tests)
- 📊 JaCoCo code coverage reporting
- 🐳 Docker support with multi-stage builds
- 🚀 CI/CD ready with Jenkins pipeline
- 📝 Structured logging with rotation
- 🔍 Spring Boot Actuator for monitoring
- 🎯 Input sanitization with OWASP AntiSamy
## Tech Stack
- **Framework**: Spring Boot 4.0.3
- **Language**: Java 25
- **Database**: PostgreSQL 16
- **Build Tool**: Gradle 9.3
- **Testing**: JUnit 5, Mockito, Spring Test
- **Security**: Spring Security, JWT (jjwt 0.12.6)
- **Database Migration**: Flyway
- **Code Coverage**: JaCoCo
- **Containerization**: Docker & Docker Compose
## Prerequisites
- Java 25 (JDK)
- Docker & Docker Compose
- Gradle (wrapper included)
## Quick Start
### 1. Clone the Repository
```bash
git clone https://github.com/vivekgupta-cse/TaskManagerApplication.git
cd TaskManagerApplication
```
### 2. Development Environment
#### Start Development Database
```bash
docker compose -f docker_scripts/docker-compose-postgres.yml up -d
```
#### Run the Application Locally
```bash
# Using Gradle
./gradlew bootRun --args='--spring.profiles.active=local'
# Or build and run JAR
./gradlew bootJar
java -jar build/libs/TaskManagerApplication.jar
```
The application will start on `http://localhost:9090`
### 3. Using Docker (Recommended)
#### Build and Run Everything
```bash
# Build the JAR
./gradlew bootJar
# Start PostgreSQL and Application
docker compose -f docker_scripts/docker-compose-postgres.yml -f docker_scripts/docker-compose-app.yml up -d
```
#### Stop Services
```bash
# Stop and preserve data
docker compose -f docker_scripts/docker-compose-postgres.yml -f docker_scripts/docker-compose-app.yml down
# Stop and remove data volumes
docker compose -f docker_scripts/docker-compose-postgres.yml -f docker_scripts/docker-compose-app.yml down -v
```
## Configuration
The application uses environment variables for sensitive configuration:
```bash
# Required Environment Variables
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/taskdb"
export SPRING_DATASOURCE_USERNAME="your_username"
export SPRING_DATASOURCE_PASSWORD="your_password"
# Optional
export SERVER_PORT=9090
```
For local development, you can create `application-local.yaml` with your credentials (this file is gitignored).
## API Documentation
### Base URL: `http://localhost:9090`
### Authentication Endpoints
#### Register User
```bash
POST /api/auth/register
Content-Type: application/json
{
  "username": "john",
  "password": "password123"
}
```
#### Login
```bash
POST /api/auth/login
Content-Type: application/json
{
  "username": "john",
  "password": "password123"
}
Response: 
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
### Task Endpoints (Authenticated)
All task endpoints require JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```
#### Get All Tasks
```bash
GET /api/tasks?page=0&size=10&sort=createdAt,desc
# Filter by title (case-insensitive partial match)
GET /api/tasks?title=groceries
# Filter by completion status
GET /api/tasks?completed=true
# Combined filters with sorting
GET /api/tasks?title=meeting&completed=false&sort=header,asc
```
#### Get Task by ID
```bash
GET /api/tasks/{id}
```
#### Create Task
```bash
POST /api/tasks
Content-Type: application/json
Authorization: Bearer <token>
{
  "title": "Buy Groceries",
  "description": "Milk, Bread, Eggs",
  "completed": false
}
```
#### Update Task
```bash
PUT /api/tasks/{id}
Content-Type: application/json
Authorization: Bearer <token>
{
  "title": "Buy Groceries - Updated",
  "description": "Milk, Bread, Eggs, Butter",
  "completed": true
}
```
#### Delete Task
```bash
DELETE /api/tasks/{id}
Authorization: Bearer <token>
Response: 204 No Content
```
### Actuator Endpoints
```bash
# Health check
GET /actuator/health
# Application info
GET /actuator/info
# Metrics
GET /actuator/metrics
```
## Testing
### Run All Tests
```bash
./gradlew test
```
### Run Tests with Coverage Report
```bash
./gradlew test jacocoTestReport
# View report
open build/reports/jacoco/test/html/index.html
```
### Test with Real PostgreSQL Database
```bash
# Start test database
docker compose -f docker_scripts/docker-compose-postgres-test.yml up -d
# Run tests
./gradlew test
# Stop test database
docker compose -f docker_scripts/docker-compose-postgres-test.yml down
```
## Database Migrations
Flyway migrations are located in `src/main/resources/db/migration/`
- Migrations run automatically on application startup
- Naming convention: `V{version}__{description}.sql`
- Example: `V1__init.sql`, `V2__add_users_table.sql`
## Project Structure
```
TaskManagerApplication/
├── src/
│   ├── main/
│   │   ├── java/com/taskmanager/app/
│   │   │   ├── config/          # Spring Security, JPA, Flyway config
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── exception/       # Custom exceptions & global handler
│   │   │   ├── mapper/          # Entity-DTO mappers (MapStruct)
│   │   │   ├── model/           # JPA entities
│   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   ├── security/        # JWT filter & authentication
│   │   │   ├── service/         # Business logic
│   │   │   └── specification/   # JPA Specifications for filtering
│   │   └── resources/
│   │       ├── application.yaml # Main configuration
│   │       └── db/migration/    # Flyway SQL scripts
│   └── test/                    # Unit & Integration tests
├── docker_scripts/              # Docker compose files
├── Jenkinsfile                  # CI/CD pipeline definition
└── README.md
```
## CI/CD with Jenkins
The project includes a complete Jenkins pipeline that:
1. ✅ Checks out code from GitHub
2. 🔨 Builds the JAR file
3. 🧪 Runs all tests
4. 🐳 Builds Docker image
5. 📤 Pushes to GCP Artifact Registry
6. 🚀 Deploys to Google Cloud Run
### Prerequisites for Jenkins
- Jenkins with Pipeline plugin
- Jenkins credentials configured:
  - `test-db-url`, `test-db-username`, `test-db-password` (for tests)
  - `gcp-service-account-json` (for GCP deployment)
  - `gcp-project-id` (GCP project ID)
  - `gcp-region` (deployment region)
## Deployment
### Google Cloud Run
```bash
# Build JAR
./gradlew bootJar
# Build Docker image
docker build -f docker_scripts/Dockerfile -t task-manager:latest .
# Configure Docker for GCP
gcloud auth configure-docker us-central1-docker.pkg.dev
# Tag and push
docker tag task-manager:latest us-central1-docker.pkg.dev/YOUR-PROJECT/task-repo/task-manager:latest
docker push us-central1-docker.pkg.dev/YOUR-PROJECT/task-repo/task-manager:latest
# Deploy to Cloud Run
gcloud run deploy task-manager \
  --image us-central1-docker.pkg.dev/YOUR-PROJECT/task-repo/task-manager:latest \
  --region us-central1 \
  --port 9090 \
  --allow-unauthenticated \
  --set-env-vars=SPRING_DATASOURCE_URL=<db-url>,SPRING_DATASOURCE_USERNAME=<user>,SPRING_DATASOURCE_PASSWORD=<pass>
```
## Security Features
- 🔐 JWT-based authentication with HS256 algorithm
- 🔒 Password encryption with BCrypt
- 🛡️ XSS protection with OWASP AntiSamy
- 🚫 SQL injection prevention via JPA/Hibernate
- ✅ Input validation using Jakarta Validation
- 🔍 Security headers configured
## Logging
Logs are written to:
- **Console**: All environments
- **File**: `logs/app.log` (rotates at 10MB, keeps 7 files)
Log levels:
- Production: INFO
- Development: DEBUG (with SQL logging)
## Data Persistence
Database data is stored in Docker volumes. To preserve data:
```bash
# Stop without removing volumes
docker compose down
# To remove data completely
docker compose down -v
```
You can also mount a local directory in `docker-compose.yml`:
```yaml
volumes:
  - ./database/postgresql/data:/var/lib/postgresql/data
```
## Performance Features
- ⚡ Virtual threads enabled (Java 21+ feature)
- 🚀 Connection pooling (HikariCP)
- 📊 Lazy loading for JPA relationships
- 🎯 Efficient pagination and sorting
- 💾 Database indexing on frequently queried fields
## Troubleshooting
### Application won't start
1. **Check database connection**:
   ```bash
   docker compose -f docker_scripts/docker-compose-postgres.yml ps
   ```
2. **Verify environment variables**:
   ```bash
   echo $SPRING_DATASOURCE_URL
   ```
3. **Check logs**:
   ```bash
   # Docker logs
   docker compose logs taskmanager-app
   # File logs
   tail -f logs/app.log
   ```
### Tests failing
1. **Ensure test database is running**:
   ```bash
   docker compose -f docker_scripts/docker-compose-postgres-test.yml up -d
   ```
2. **Check test database connection**:
   ```bash
   docker compose -f docker_scripts/docker-compose-postgres-test.yml ps
   ```
3. **Run with debug logging**:
   ```bash
   ./gradlew test --info
   ```
### Port already in use
```bash
# Change port in environment variable
export SERVER_PORT=8080
# Or in application.yaml
server:
  port: 8080
```
## Contributing
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
## License
This project is licensed under the MIT License - see the LICENSE file for details.
## Contact
For questions or support, please open an issue on GitHub.

---

## 🆕 Usage Instructions for Observability & Request ID Propagation (2026-03-16)

### 1. X-Request-Id Propagation
- Every HTTP request is checked for an `X-Request-Id` header.
- If present, the same value is echoed in the response and used for logging/tracing.
- If missing, a new UUID is generated, set in the response, and used for logging/tracing.
- **How to use:**
  - Clients may send `X-Request-Id` in requests for traceability.
  - All responses will include `X-Request-Id`.

### 2. Prometheus & Grafana for Local Observability
- **Start Prometheus & Grafana:**
  ```bash
  docker compose -f docker_scripts/docker-compose-observability.yml up -d
  ```
- **Prometheus config:** Scrapes metrics from your app at `/actuator/prometheus` (see `docker_scripts/prometheus.yml`).
- **Grafana:**
  - Access at [http://localhost:3000](http://localhost:3000) (default admin password: `admin`).
  - Add Prometheus as a data source (URL: `http://prometheus:9090` inside Docker, or `http://localhost:9091` from host).

### 3. OpenTelemetry Collector (OTLP) for Tracing
- **Start OTEL Collector:**
  ```bash
  docker compose -f docker_scripts/docker-compose-otel.yml up -d
  ```
- **Config:** Minimal config at `docker_scripts/otel-collector-config.yaml` using the OTEL `debug` exporter (prints traces to collector stdout).
- **Point your app to the collector:**
  ```bash
  export OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4318/v1/traces"
  # Then start your app as usual
  ```
- **Result:** Traces from your app will appear in the OTEL Collector logs.

- **Use one of these commands to view the logs:**
  ```bash
     docker logs otel-collector > otel.log 2>&1
     docker compose -f docker_scripts/docker-compose-otel.yml logs -f
  ```
- You should be able to see the trace id and span id from app.log in the otel logs.

### 4. Example: Running Everything Together
```bash
# Start DB, App, Observability, and OTEL Collector
# (in separate terminals or with multiple compose files)
docker compose -f docker_scripts/docker-compose-postgres.yml up -d
docker compose -f docker_scripts/docker-compose-app.yml up -d
docker compose -f docker_scripts/docker-compose-observability.yml up -d
docker compose -f docker_scripts/docker-compose-otel.yml up -d
```

---

For more details, see the respective files in `docker_scripts/`.

**Note**: Ensure you never commit sensitive information like passwords or API keys. Use environment variables or local configuration files (which are gitignored).
