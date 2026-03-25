## Running Local

> **Note**: The Admin is a terminal application, and cannot run inside other tools such as IntelliJ terminal or via Gradle
run.

From the root folder:

- run `./gradlew` to build the code
- run `./gradlew :cluster:run` to start a single-node cluster
- run `java -jar admin/build/libs/admin-uber.jar`
- type `connect` to connect to the cluster

## Admin Commands

> **Note**: The cluster starts up with two participants, `500` (`initiator`) and `501` (`responder`) preconfigured.

You can obtain per command help by typing `help` followed by the command name.

-   `connect` - connect to a cluster. Optional arguments are `hostnames` and `baseport`. Defaults are `localhost`
    and `9000`.
-   `disconnect` - disconnect from the cluster.
-   `add-participant` - adds a participant to the cluster. Arguments are `id` and `name`.
-   `list-participants` - lists all participants in the cluster.
-   `add-auction` - adds an auction to the cluster starting in 0.1 seconds and ending 25 seconds later. `name` is a
    required argument, created by is optional.
-   `list-auctions` - lists all auctions in the cluster.
-   `add-bid` - adds a bid to the cluster. Arguments are `id`, `participant-id`, `price`.
-   `help` - show help.
-   `exit` - exit the application.

Sample happy path script:

```
connect
add-auction created-by=500 name=Tulips
<assumes auction ID 1 is logged>
add-bid auction-id=1 created-by=501 price=1000
disconnect
exit
```

## Protocol Notes

The admin uses a simple protocol via SBE to communicate from the CLI commands to an Agrona Agent running the cluster communications.
This Agrona agent then converts from the CLI SBE protocol to the cluster SBE protocol.
This approach is typical for gateways, for example you may have a web socket gateway that uses a json protocol, and then a cluster-specific protocol from the gateway to the cluster.

## Environment Variables

| Variable          | Description                                                                       | Default     |
| ----------------- | --------------------------------------------------------------------------------- | ----------- |
| AUTO_CONNECT      | If set to `true`, the admin will automatically connect to the cluster on startup. | `false`     |
| PARTICIPANT_ID    | The participant ID to use when connecting to the cluster.                         | `0`         |
| DUMB_TERMINAL     | If set to `true`, the admin will not use ANSI escape codes for terminal output.   | `false`     |
| CLUSTER_ADDRESSES | A comma separated list of cluster addresses to connect to.                        | `localhost` |

## Uber Jar Manifest notes

-   `Main-Class: io.aeron.samples.admin.Admin`
-   `Add-Opens: java.base/sun.nio.ch java.base/jdk.internal.misc java.base/java.util.zip`
