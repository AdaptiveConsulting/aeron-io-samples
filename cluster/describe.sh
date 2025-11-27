#!/bin/sh
java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED  -cp /home/aeron/jar/aeron-all-*.jar io.aeron.cluster.ClusterTool /home/aeron/jar/aeron-cluster/cluster describe
