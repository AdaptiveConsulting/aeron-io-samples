#!/bin/sh

java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -Djava.net.preferIPv4Stack=true "$@" -jar /root/jar/cluster-uber.jar
