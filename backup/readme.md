# Backup
The Backup application demonstrates how to replicate cluster data to another site, and how to use this
data to populate new cluster nodes, or recover cluster nodes after a failure.

The commands below, which should be run from the `docker` directory, demonstrate how you can do this.

## 1. Create Data
First, start up all the services, and use the Admin interface to create an Auction.

```bash
docker compose up -d
docker exec -i  aeron-admin1-1 java -jar admin-uber.jar
```

```
admin > add-auction created-by=500 name=Tulips duration=1800
Auction added with id: 1
New auction: 'Tulips' (1)
Auction 1 is now in state OPEN. There have been 0 bids.
```

We want to demonstrate that we can replicate snapshots, so we will trigger a snapshot on the leader.

```bash
docker exec -it $(./docker_find_leader.sh) /root/jar/snapshot.sh
```

We also want to demonstrate replicating cluster log that follows a snapshot, so let's create another
Auction using the Admin interface.

```
admin > add-auction created-by=500 name=Daffodils duration=1800
Auction added with id: 2
New auction: 'Daffodils' (2)
Auction 2 is now in state OPEN. There have been 0 bids.
```

Snapshots are replicated every minute. Have a look in the logs on the backup node to make sure the
snapshot has been retrieved, and then create a tarball of the `archive` and `cluster` directories
and copy it out of the container.

```bash
docker exec -it aeron-backup-1 tar cvzf /root/jar/cluster_backup.tgz -C /root/jar/backup archive/ cluster/
docker cp aeron-backup-1:/root/jar/cluster_backup.tgz .
```

Next, shutdown the containers are remove the volumes, then restart the containers in a fresh state.

```bash
docker compose down --volumes
docker compose up -d
```

We can verify that the cluster doesn't have any state by listing the Auctions using the Admin interface.

```bash
docker exec -i  aeron-admin1-1 java -jar admin-uber.jar
```

```
admin > list-auctions
No auctions exist in the cluster. Closed auctions are deleted automatically.
```

Now, let's stop the containers, restore the data from the backup, and start the containers up again.

```bash
docker compose stop
for i in {0..2}; do zcat cluster_backup.tgz | docker cp - aeron-node${i}-1:/root/jar/aeron-cluster-${i}; done
docker compose up -d
```

We can see that the data has been restored by again listing the Auctions with the Admin interface.

```bash
docker exec -i  aeron-admin1-1 java -jar admin-uber.jar
```

```
admin > list-auctions
Auction count: 2
Auction 'Tulips' with id 1 created by 500 is now in state PRE_OPEN
Auction 'Daffodils' with id 2 created by 500 is now in state OPEN
```
