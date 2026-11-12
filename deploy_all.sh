cd kubernetes_scripts
kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres-deployment.yaml
kubectl apply -f postgres-service.yaml

cd ../
./gradlew clean bootJar
docker build -t taskmanager:v1 .

cd kubernetes_scripts
kubectl apply -f app-deployment.yaml
kubectl apply -f app-service.yaml


kubectl get all -n taskmanager
