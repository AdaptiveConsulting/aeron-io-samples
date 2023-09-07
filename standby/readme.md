# Standby

Aeron Cluster Standby is a premium feature.
In order to the Standby quickstart you must have access to the binaries Aeron Cluster Standby provided by Adaptive.
If you don't have access to these this feature interests you please contact adaptive via http://aeron.io.

The Standby features of the quickstart a disabled by default.
They need to be enabled in both the build and with any docker compose steps.
E.g. for builds within the project root.

```shell
> ./gradlew -Pstandby=true
```

When running the docker steps within the `<project root>/docker` directory
```shell
> docker compose --profile standby build --no-cache
> docker compose --profile standby up
```
