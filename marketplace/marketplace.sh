#!/bin/sh
java -jar /home/aeron/jar/marketplace-uber.jar
exec /home/aeron/jar/entrypoint.sh "$@"
