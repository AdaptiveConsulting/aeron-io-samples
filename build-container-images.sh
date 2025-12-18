echo "building admin image..."
docker build -f docker/Dockerfile --no-cache -t admin --build-context gradle=admin admin
echo "building cluster image..."
docker build -f docker/Dockerfile --no-cache -t cluster --build-context gradle=cluster cluster
