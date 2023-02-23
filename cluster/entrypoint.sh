#!/bin/sh
java -Djava.net.preferIPv4Stack=true "$@" -jar /root/jar/cluster-uber.jar
