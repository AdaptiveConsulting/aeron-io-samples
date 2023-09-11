# Standby

Aeron Cluster Standby is a premium feature.
To use the Standby quickstart, you need access to the Aeron Cluster Standby binaries provided by Adaptive.
If you don't have access but are interested, please contact Adaptive at http://aeron.io.

The Standby features in the quickstart are disabled by default.
To activate them, you must enable them during the build process and in any Docker Compose steps.

For example, when building from the project root:

```shell
./gradlew -Pstandby=true
```

And to run Standby with docker:

```shell
cd docker
docker compose --profile standby build --no-cache
docker compose --profile standby up
```
