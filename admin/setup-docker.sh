#!/bin/bash

echo "debconf debconf/frontend select noninteractive" | debconf-set-selections

apt-get update --quiet

apt-get install \
    --quiet \
    --assume-yes \
    --no-install-recommends \
    bash

