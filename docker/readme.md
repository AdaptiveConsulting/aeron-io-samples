# Docker

Assumes Docker Compose 2 is available.

## Building containers

`docker compose build --no-cache`

## Running containers

`docker compose up -d`

## Connecting to the admin container

(assumes a container named `docker-admin-1` is running)

`docker exec -it docker-admin-1 bash`

Once you are in the container, you can run the admin with:

```bash
java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar admin-uber.jar
```

Within the admin, you can connect to the cluster with:

```bash
connect hostnames=172.16.202.2,172.16.202.3,172.16.202.4
```

## Stopping containers

`docker compose down`

## Tooling in containers

In the `~/aeron` folder, you can find some aeron tools, including AeronStat.

For example, to open AeronStat, move to `~/aeron` and run:

```bash
java -cp ~/aeron/aeron-all-*.jar -Daeron.dir=/dev/shm/aeron-root-0-driver io.aeron.samples.AeronStat
```