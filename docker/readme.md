# Docker

Assumes Docker Compose 2 is available.

## Building containers

- build the source code by running `./gradlew` in the project root directory
- build the containers with `docker compose build --no-cache`

## Running containers

`docker compose up -d`

## Finding the cluster leader

You can either review the logs of the containers with `docker compose logs` and search for `LEADER` (e.g. `docker compose logs | grep LEADER`), or use the script `./docker_find_leader.sh`.

## Connecting to the admin container

(assumes a container named `docker-admin-1` is running)

`docker exec -it docker-admin-1 java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar admin-uber.jar`

Within the admin, you can then connect to the cluster:

```bash
connect
```

## Stopping containers

`docker compose down`

## Tooling in containers

The cluster containers contain a number of tool scripts, such as `aeronstat-single.sh` and `snapshot.sh`.
