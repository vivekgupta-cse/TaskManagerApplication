#!/bin/bash -x

NS=taskmanager
kubectl delete all --all -n $NS
kubectl delete pvc --all -n $NS