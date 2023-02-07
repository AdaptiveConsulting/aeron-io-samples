#
# Copyright (c) 2023 Adaptive Financial Consulting
#

echo "🏛️  building Java..."
./gradlew
echo "🐳️  building admin docker image..."
cd admin
docker build . -t admin --no-cache
echo "🐳  building cluster image..."
cd ../cluster
docker build . -t cluster --no-cache
echo "🔥  removing old kubernetes namespaces..."
kubectl delete ns aeron-io-sample-admin
kubectl delete ns aeron-io-sample-cluster
echo "📷  loading images..."
minikube image load admin:latest
minikube image load cluster:latest
echo "▶️  applying admin..."
cd ./kubernetes/admin
kubectl apply -f .
echo "▶️  applying cluster..."
cd ../cluster
kubectl apply -f .