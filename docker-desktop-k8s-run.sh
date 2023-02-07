#
# Copyright (c) 2023 Adaptive Financial Consulting
#

echo "🏛️️  building Java..."
./gradlew
echo "🐳️  building admin docker image..."
cd admin || exit
docker build . -t admin --no-cache
echo "🐳  building cluster image..."
cd ../cluster || exit
docker build . -t cluster --no-cache
cd .. || exit
echo "🔥  removing old kubernetes namespaces..."
kubectl delete ns aeron-io-sample-admin
kubectl delete ns aeron-io-sample-cluster
echo "▶️  applying admin..."
cd ./kubernetes/admin || exit
kubectl apply -f .
echo "▶️  applying cluster..."
cd ../cluster || exit
kubectl apply -f .