#
# Copyright (c) 2023 Adaptive Financial Consulting
#

cd admin
docker build . -t admin --no-cache
cd ../cluster
docker build . -t cluster --no-cache