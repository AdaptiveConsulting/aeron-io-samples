if ! [ -x "$(command -v docker)" ]; then
  echo 'Error: docker is not installed. Please install it first.' >&2
  exit 1
fi

if ! [ -x "$(command -v kubectl)" ]; then
  echo 'Error: kubectl is not installed. Please install it first.' >&2
  exit 1
fi

echo "Building Java..."
./gradlew
echo "Building docker images..."
cd docker || exit
docker compose build
cd .. || exit
echo "Removing old kubernetes namespaces (if they exist)..."
kubectl delete ns aeron-io-sample-admin
kubectl delete ns aeron-io-sample-cluster
echo "Applying admin..."
cd ./kubernetes/admin || exit
kubectl apply -f .
echo "Applying cluster..."
cd ../cluster || exit
kubectl apply -f .
echo "Done"
