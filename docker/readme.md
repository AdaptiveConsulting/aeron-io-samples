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
./run-admin.sh
```

Within the admin, you can then connect to the cluster:

```bash
connect
```

## Stopping containers

`docker compose down`

## Tooling in containers

In the `~/aeron` folder, you can find some aeron tools, including AeronStat.

For example, to open AeronStat, move to `~/aeron` and run:

```bash
java -cp ~/aeron/aeron-all-*.jar -Daeron.dir=/dev/shm/aeron-root-0-driver io.aeron.samples.AeronStat
```
