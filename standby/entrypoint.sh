#!/bin/sh
java -Djava.net.preferIPv4Stack=true -Daeron.ipc.mtu.length=8k "$@" -jar /home/aeron/jar/standby-uber.jar
