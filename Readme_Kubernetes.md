# TaskManagerApplication on Local Kubernetes (Rancher Desktop)

## Objective
Run a Spring Boot application and PostgreSQL database on a local Kubernetes cluster provided by Rancher Desktop (k3s).

## Starting Point
The application was already working locally using:

```bash
docker compose -f docker_scripts/docker-compose-postgres.yml up -d
./gradlew bootJar
java -jar build/libs/TaskManagerApplication.jar
```

## Verify Kubernetes

```bash
kubectl get nodes
kubectl cluster-info
```

Expected:

```text
STATUS: Ready
```

## Create Namespace

```bash
kubectl create namespace taskmanager
kubectl get namespaces
```

## Deploy PostgreSQL Storage

Create `postgres-pvc.yaml`:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: taskmanager
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

Apply:

```bash
kubectl apply -f postgres-pvc.yaml
```

## Deploy PostgreSQL

Create `postgres-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: taskmanager
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:16
        env:
        - name: POSTGRES_USER
          value: docker
        - name: POSTGRES_PASSWORD
          value: docker
        - name: POSTGRES_DB
          value: taskdb
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-data
          mountPath: /var/lib/postgresql/data
      volumes:
      - name: postgres-data
        persistentVolumeClaim:
          claimName: postgres-pvc
```

Apply:

```bash
kubectl apply -f postgres-deployment.yaml
```

## Create PostgreSQL Service

Create `postgres-service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: taskmanager
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
```

Apply:

```bash
kubectl apply -f postgres-service.yaml
```

## Make Spring Boot Database URL Configurable

Original:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/taskdb
```

Updated:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/taskdb}
    username: ${DB_USERNAME:docker}
    password: ${DB_PASSWORD:docker}
```

## Build Spring Boot JAR

```bash
./gradlew clean bootJar
```

## Create Dockerfile

Because the application is built with Java 25:

```dockerfile
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java","-jar","app.jar"]
```

## Build Docker Image

Run from project root:

```bash
docker build -t taskmanager:v1 .
```

## Deploy Spring Boot

Create `app-deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: taskmanager
  namespace: taskmanager
spec:
  replicas: 1
  selector:
    matchLabels:
      app: taskmanager
  template:
    metadata:
      labels:
        app: taskmanager
    spec:
      containers:
      - name: taskmanager
        image: taskmanager:v1
        imagePullPolicy: Never
        ports:
        - containerPort: 9090
        env:
        - name: DB_URL
          value: jdbc:postgresql://postgres:5432/taskdb
        - name: DB_USERNAME
          value: docker
        - name: DB_PASSWORD
          value: docker
```

Apply:

```bash
kubectl apply -f app-deployment.yaml
```

## Expose Spring Boot

Create `app-service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: taskmanager
  namespace: taskmanager
spec:
  selector:
    app: taskmanager
  ports:
  - port: 9090
    targetPort: 9090
  type: NodePort
```

Apply:

```bash
kubectl apply -f app-service.yaml
```

## Verify Deployment

```bash
kubectl get all -n taskmanager
```

Expected components:

- postgres pod running
- taskmanager pod running
- postgres service
- taskmanager NodePort service

## Access Application

Find NodePort:

```bash
kubectl get svc -n taskmanager
```

Example:

```text
taskmanager   NodePort   9090:32232/TCP
```

Access:

```text
http://localhost:32232
```

Use Bruno to call APIs.

## Useful Debug Commands

Application logs:

```bash
kubectl logs -f deployment/taskmanager -n taskmanager
```

PostgreSQL logs:

```bash
kubectl logs -f deployment/postgres -n taskmanager
```

Describe pod:

```bash
kubectl describe pod <pod-name> -n taskmanager
```

Open shell in application pod:

```bash
kubectl exec -it deployment/taskmanager -n taskmanager -- sh
```

Open shell in PostgreSQL pod:

```bash
kubectl exec -it deployment/postgres -n taskmanager -- bash
```

Port-forward PostgreSQL for DBeaver:

```bash
kubectl port-forward svc/postgres 5432:5432 -n taskmanager
```

Port-forward Spring Boot:

```bash
kubectl port-forward svc/taskmanager 9090:9090 -n taskmanager
```

## Debugging with IDE Breakpoints

Recommended development workflow:

1. Keep PostgreSQL running in Kubernetes.
2. Forward database port:

```bash
kubectl port-forward svc/postgres 5432:5432 -n taskmanager
```

3. Scale application down:

```bash
kubectl scale deployment taskmanager --replicas=0 -n taskmanager
```

4. Run Spring Boot from IntelliJ in Debug mode.
5. Use Bruno against `http://localhost:9090`.
6. Breakpoints work normally while continuing to use PostgreSQL from Kubernetes.
 