#!/bin/sh
aerondir=$(ls /dev/shm/ | grep aeron | head -1)
java -cp /home/aeron/jar/aeron-all-*.jar -Daeron.dir=/dev/shm/$aerondir io.aeron.samples.AeronStat watch=false
