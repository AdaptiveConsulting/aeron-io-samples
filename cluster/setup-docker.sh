#!/bin/bash

echo "debconf debconf/frontend select noninteractive" | debconf-set-selections

apt-get update \
    --quiet \
    --assume-yes

apt-get install \
    --quiet \
    --assume-yes \
    --no-install-recommends \
    bash \
    procps \
    less \
    sysstat
