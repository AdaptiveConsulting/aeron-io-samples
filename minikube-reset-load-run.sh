#
# Copyright (c) 2023 Adaptive Financial Consulting
#

echo "removing old namespaces..."
kubectl delete ns aeron-io-sample-admin
kubectl delete ns aeron-io-sample-cluster
echo "loading images..."
minikube image load admin:latest
minikube image load cluster:latest
echo "applying admin..."
cd ./kubernetes/admin
kubectl apply -f .
echo "applying cluster..."
cd ../cluster
kubectl apply -f .