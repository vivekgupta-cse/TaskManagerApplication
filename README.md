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

Steps to deploy to GCP:
1. ./gradlew bootJar
2. docker build -t task-manager:v1 .
3. gcloud auth configure-docker us-central1-docker.pkg.dev 
4. docker tag task-manager:v1 us-central1-docker.pkg.dev/task-manager-demo-489313/task-repo/task-manager:v1
5. gcloud services enable run.googleapis.com
6. gcloud services enable artifactregistry.googleapis.com
7. gcloud artifacts repositories create task-repo \
   --repository-format=docker \
   --location=us-central1 \
   --description="Docker repository for my task manager app"
8. docker push us-central1-docker.pkg.dev/task-manager-demo-489313/task-repo/task-manager:v1
9. gcloud run deploy task-manager \
   --image us-central1-docker.pkg.dev/task-manager-demo-489313/task-repo/task-manager:v1 \
   --region us-central1 \
   --port 9090 \
   --allow-unauthenticated \
   --set-env-vars=SPRING_DATASOURCE_URL=jdbc:postgresql://ep-green-bird-a1iew4mc-pooler.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require,SPRING_DATASOURCE_USERNAME=neondb_owner,SPRING_DATASOURCE_PASSWORD=npg_BAZSyp2De5dP




Jenkins Deployment steps:
1. if you want to delete old Jenkins data and setting and start afresh

    docker volume rm jenkins_home

2. Start Jenkins in Docker

   docker run --name jenkins -p 8181:8080 -p 50000:50000 -v jenkins_home:/var/jenkins_home jenkins/jenkins:lts

3. To get initial password

    docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword 

4. Install pipeline and git plugins

5. To disable security (Temporary step, need to fix):

    docker run --rm -v jenkins_home:/var/jenkins_home alpine sed -i 's/<useSecurity>true<\/useSecurity>/<useSecurity>false<\/useSecurity>/g' /var/jenkins_home/config.xml

6. 