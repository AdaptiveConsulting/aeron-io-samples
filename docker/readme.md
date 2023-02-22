# Docker

> **Note**: Assumes Docker Compose 2 is available.

This will start a three-node cluster, with 1 admin container.
The cluster nodes will run an election, selecting one of the nodes as the leader.

Containers will be named as follows:

| Container        | Description  |
|------------------|--------------|
| docker-engine0-1 | Cluster Node |
| docker-engine1-1 | Cluster Node |
| docker-engine2-1 | Cluster Node |
| docker-admin-1   | Admin Node   |

## Building containers

- First, build the source code by running `./gradlew` in the project root directory
- Then, build the containers with `docker compose build --no-cache`

## Running containers

Start the cluster and admin containers by running `docker compose up -d`

## Finding the cluster leader

To find the leader (e.g. to stop it to test failover), you can either review the logs of the containers with `docker compose logs` and search for `LEADER` (
e.g. `docker compose logs | grep LEADER`), or use the script `./docker_find_leader.sh`.

## Connecting to the admin container

(assumes a container named `docker-admin-1` is running)

`docker exec -it docker-admin-1 java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar admin-uber.jar`

Within the admin, you can then connect to the cluster:

```bash
connect
```

## Stopping containers

To stop a specific container, run:

`docker compose stop <container name>` where `<container name>` matches the name of the container you want to stop.

To stop all the containers, and remove any networking changes, run:

`docker compose down`

## Tooling in containers

The cluster containers contain a number of Aeron operations tools. These are available via scripts such as `aeronstat-single.sh` and `snapshot.sh`.
