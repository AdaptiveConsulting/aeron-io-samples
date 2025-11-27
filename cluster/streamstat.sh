#!/bin/sh
aerondir=$(ls /dev/shm/ | grep aeron | head -1)
java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -cp /home/aeron/jar/aeron-all-*.jar -Daeron.dir=/dev/shm/$aerondir io.aeron.samples.StreamStat
