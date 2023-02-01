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

You can obtain per command help by typing `help` followed by the command name.

- `help` - show help.
- `connect` - connect to a cluster. Optional arguments are `hostnames` and `baseport`. Defaults are `localhost` and `9000`.
- `disconnect` - disconnect from the cluster.
- `add-participant` - adds a participant to the cluster. Optional arguments are `id` and `name`. Defaults are `-1` and `_`.
- `add-auction` - adds an auction to the cluster starting in 10 seconds and ending 45 seconds later. Arguments are `created-by` and `name`. Defaults are `-1` and `_`.
- `add-bid` - adds a bid to the cluster. Arguments are `id`, `participant-id`, `price`. Defaults are `-1`, `-1`, `0`, `-1` and `_`.

Sample happy path script:

```
connect
add-participant id=500 name=initiator
add-participant id=501 name=responder
add-auction created-by=500 name=auction
(copy the auction id from the result; await auction open event)
add-bid id=<auction id> created-by=501 price=1000
disconnect
exit
```