# TaskManagerApplication — Quick Start

A small, production-oriented Spring Boot REST service for managing tasks.

## Prerequisites
- Docker & Docker Compose v2
- Gradle wrapper (included) — use `./gradlew`

## Quick start (Development)
1. Build the jar:

    ./gradlew bootJar

2. Start app + DB (uses `docker_scripts/docker-compose-postgres.yml` and `docker_scripts/docker-compose-app.yml`):

    docker compose -f ./docker_scripts/docker-compose-postgres.yml -f ./docker_scripts/docker-compose-app.yml up --build -d

3. Stop (preserve DB data):

    docker compose -f ./docker_scripts/docker-compose-postgres.yml -f ./docker_scripts/docker-compose-app.yml down

## Test (using dockerised Postgres)
1. Start test DB:

    docker compose -f ./docker_scripts/docker-compose-postgres-test.yml up -d

2. Run tests:

    ./gradlew test

3. Stop test DB:

    docker compose -f ./docker_scripts/docker-compose-postgres-test.yml down

## Production (simple Docker)
1. Build production jar:

    ./gradlew bootJar

2. Start production compose (or use your orchestrator):

    docker compose -f ./docker_scripts/docker-compose-postgres.yml -f ./docker_scripts/docker-compose-app.yml up --build -d

## API (base: http://localhost:9090)
- GET  /api/tasks
  - List tasks (supports `title`, `completed`, and Spring Data pagination and sorting via the `sort` parameter)
  - Example: `GET /api/tasks?title=Groceries&page=0&size=10&sort=header,asc`

- GET  /api/tasks/{id}
  - Get task by id

- POST /api/tasks
  - Create task
  - Body example: `{ "title": "Buy Groceries", "description": "Milk", "completed": false }`

- PUT  /api/tasks/{id}
  - Update task

- DELETE /api/tasks/{id}
  - Delete task (returns 204 No Content)

## Actuator
- Common endpoint: `/actuator/health` (exposed if Actuator is enabled in configuration).

## Data persistence
- The compose files use Docker volumes for Postgres data. Avoid `docker compose down -v` if you want to keep data.
- You can mount a local host folder (e.g. `./database/postgresql/data`) to persist DB files inside the repository.

## Essential commands

    ./gradlew bootJar
    docker compose -f ./docker_scripts/docker-compose-postgres.yml -f ./docker_scripts/docker-compose-app.yml up --build -d
    docker compose -f ./docker_scripts/docker-compose-postgres.yml -f ./docker_scripts/docker-compose-app.yml logs -f taskmanager-app
    docker compose -f ./docker_scripts/docker-compose-postgres.yml -f ./docker_scripts/docker-compose-app.yml down
    docker compose -f ./docker_scripts/docker-compose-postgres-test.yml up -d
    ./gradlew test

That's it — minimal and ready for users to follow.
