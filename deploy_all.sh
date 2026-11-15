kubectl apply -f kubernetes_scripts/postgres-pvc.yaml
kubectl apply -f kubernetes_scripts/postgres-deployment.yaml
kubectl apply -f kubernetes_scripts/postgres-service.yaml

chmod 755 ./gradlew
./gradlew clean bootJar
docker build -t taskmanager:v1 .

kubectl apply -f kubernetes_scripts/app-deployment.yaml
kubectl apply -f kubernetes_scripts/app-service.yaml


kubectl get all -n taskmanager

kubectl port-forward svc/taskmanager 9090:9090 -n taskmanager
