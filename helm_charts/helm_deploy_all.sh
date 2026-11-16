#!/bin/bash -x

cd ../
chmod 755 ./gradlew
./gradlew clean bootJar
docker build -t taskmanager:v1 .
cd helm_charts

NS=taskmanager
kubectl create namespace $NS



helm install taskmanager ./taskmanager --namespace $NS


kubectl get all -n $NS
kubectl port-forward svc/taskmanager 9090:9090 -n $NS
