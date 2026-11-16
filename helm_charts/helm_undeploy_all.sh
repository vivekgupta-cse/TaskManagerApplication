#!/bin/bash -x

NS=taskmanager
helm uninstall taskmanager --namespace $NS
