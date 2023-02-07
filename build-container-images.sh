#
# Copyright (c) 2023 Adaptive Financial Consulting
#

echo "building admin image..."
cd admin
docker build . -t admin --no-cache
echo "building cluster image..."
cd ../cluster
docker build . -t cluster --no-cache