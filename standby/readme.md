# Standby

Aeron Cluster Standby is a premium feature.
In order to the Standby quickstart you must have access to the binaries Aeron Cluster Standby provided by Adaptive.
If you don't have access to these this feature interests you please contact Adaptive via http://aeron.io.

The Standby features of the quickstart are disabled by default.
They need to be enabled in both the build and with any docker compose steps.
E.g. for builds within the project root.

```shell
./gradlew -Pstandby=true
```

To run Standby with docker:
```shell
cd docker
docker compose --profile standby build --no-cache
docker compose --profile standby up
```
