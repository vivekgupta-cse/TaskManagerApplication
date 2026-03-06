# Runtime-only Dockerfile for TaskManagerApplication
# Expects the Spring Boot fat jar to be present at build time in build/libs/*.jar
ARG BASE_RUNTIME_IMAGE=amazoncorretto:25
FROM ${BASE_RUNTIME_IMAGE}
WORKDIR /app

# Copy the pre-built Spring Boot jar (run ./gradlew bootJar before docker build)
COPY build/libs/*.jar app.jar

# Expose configured server port (application.yaml uses 9090)
EXPOSE 9090

# NOTE: For local development we run as root inside the container to avoid user creation issues
# For production images create a proper non-root user in a robust way.

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/app.jar"]
