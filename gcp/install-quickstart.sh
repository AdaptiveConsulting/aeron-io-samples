#!/usr/bin/env bash
set -e

cd /opt
git clone https://github.com/AdaptiveConsulting/aeron-io-samples.git
chown -R ubuntu:ubuntu aeron-io-samples
runuser -l ubuntu -c "cd /opt/aeron-io-samples && ./gradlew"
runuser -l ubuntu -c "cd /opt/aeron-io-samples/docker && docker compose build --no-cache"
