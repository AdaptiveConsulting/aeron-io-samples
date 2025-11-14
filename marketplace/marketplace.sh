#!/bin/sh
java -jar /home/aeron/jar/marketplace-uber.jar
if [ $? -ne 0 ]; then
    echo "Error: license check failed. Exiting."
    exit 1
fi
exec /home/aeron/jar/entrypoint.sh "$@"
