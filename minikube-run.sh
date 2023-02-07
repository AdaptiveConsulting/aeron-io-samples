#
# Copyright (c) 2023 Adaptive Financial Consulting
#

echo "building"
./gradlew
./build-container-images.sh
./minikube-reset-load-run.sh