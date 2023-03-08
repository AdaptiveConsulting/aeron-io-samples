## Running Local

- run `./gradlew` to build the code
- run `./gradlew runSingleNodeCluster`

## Environment Variables

| Variable | Description                                                                                     | Default     |
|----------|-------------------------------------------------------------------------------------------------|-------------|
| CLUSTER_PORT_BASE | The base port to use for the cluster.                                                           | `9000`      |
| CLUSTER_NODE | The cluster node index in the CLUSTER_ADDRESSES comma separated list that this node represents. | `0`         |
| CLUSTER_ADDRESSES | A comma separated list of cluster addresses to connect to.                                      | `localhost` |

## Uber Jar Manifest notes

- `Main-Class: io.aeron.samples.cluster.Cluster`
- `Add-Opens: java.base/sun.nio.ch`
