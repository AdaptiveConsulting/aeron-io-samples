> **Note**: You will need a running cluster for the Admin to connect to. `./gradlew runSingleNodeCluster` will start a cluster.

## Running Admin

First, you will need to build the uberJar. You can do this via gradle:

```bash
./gradlew uberJar
```

This should output an uber jar in:

`/admin/build/libs/admin-uber.jar`

Then you can move to that folder and run admin with:

```bash
java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar admin-uber.jar
```

Note that the admin is a terminal application, and cannot run inside other tools such as IntelliJ terminal or via Gradle run.

## Commands

- `connect` - connect to a cluster. Optional arguments are `hostnames` and `baseport`. Defaults are `localhost` and `9000`.
- `disconnect` - disconnect from the cluster.
- `add-participant` - adds a participant to the cluster. Optional arguments are `id` and `name`. Defaults are `-1` and `_`.
- `add-auction` - adds an auction to the cluster starting in 10 seconds and ending 30 seconds later. Arguments are `created-by` and `name`. Defaults are `-1` and `_`.