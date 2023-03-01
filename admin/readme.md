# Admin

## Admin Commands

> Note: The cluster starts up with two participants, `500` (`initiator`) and `501` (`responder`) preconfigured. 

You can obtain per command help by typing `help` followed by the command name.

- `connect` - connect to a cluster. Optional arguments are `hostnames` and `baseport`. Defaults are `localhost` and `9000`.
- `disconnect` - disconnect from the cluster.
- `add-participant` - adds a participant to the cluster. Arguments are `id` and `name`.
- `list-participants` - lists all participants in the cluster.
- `add-auction` - adds an auction to the cluster starting in 0.1 seconds and ending 30 seconds later. Arguments are `created-by` and `name`.
- `list-auctions` - lists all auctions in the cluster.
- `add-bid` - adds a bid to the cluster. Arguments are `id`, `participant-id`, `price`.
- `help` - show help.
- `exit` - exit the application.

Sample happy path script:

```
connect
add-auction created-by=500 name=tulips
<auction ID 1 is logged>
add-bid auction-id=1 created-by=501 price=1000
disconnect
exit
```

add-bid auction-id=2 created-by=501 price=1000

## Running Admin outside of Kubernetes or Docker

> **Note**: You will need a running cluster for the Admin to connect to. `./gradlew runSingleNodeCluster` will start a cluster.

First, you will need to build the uber jar. You can do this via gradle in the project root directory:

```bash
./gradlew
```

This should output an Admin uber jar in:

`/admin/build/libs/admin-uber.jar`

Then you can move to that folder and run admin with:

```bash
java -jar admin-uber.jar
```

Note that the admin is a terminal application, and cannot run inside other tools such as IntelliJ terminal or via Gradle run.
