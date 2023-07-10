# Backup
The Backup application demonstrates how to replicate cluster data to another site, and how to use this
data to populate new cluster nodes, or recover cluster nodes after a failure.

The instructions below demonstrate how you can do this using either Docker directly or with Kubernetes.

## Using Docker
The commands below should be run from the `docker` directory.

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
snapshot has been retrieved.

```bash
docker logs aeron-backup-1 -f
```

You should see log messages indicating that data has been copied.

```
Response from Cluster. Log Source Member: 2. Cluster Members: [0. 172.16.202.2:9003 (not leader), 1. 172.16.202.3:9103 (not leader), 2. 172.16.202.4:9203 (leader)]. Snapshots to retrieve: [Snapshot recordingId: 0, Snapshot recordingId: 1]
Updating log for recording [recordingId: 2, logPosition: 544, recordingId: 1, logPosition: 544, recordingId: 3, logPosition: -1]. Snapshots retrieved: [Snapshot recordingId: 1, Snapshot recordingId: 2]
Reached position 1408 in recording 3
```

## 2. Create a backup
Create a backup tarball of the `archive` and `cluster` directories on the Backup node, 
and copy it out of the container.

```bash
docker exec -it aeron-backup-1 tar cvzf /root/jar/cluster_backup.tgz -C /root/jar/backup archive/ cluster/
docker cp aeron-backup-1:/root/jar/cluster_backup.tgz .
```

## 3. Restart the system with clean state
Next, shutdown the containers and remove the volumes. Then, restart the containers in a fresh state.

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

## 4. Restore from backup
Now, let's stop the containers, restore the data from the backup, and start the containers up again.

```bash
docker compose stop
for i in {0..2}; do zcat cluster_backup.tgz | docker cp - aeron-node${i}-1:/root/jar/aeron-cluster; done
docker compose up -d
```

## 5. Verify the restore was successful
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


---

## Using Kubernetes
The commands below should be run from the root directory.

## 1. Create Data
First, start up all the services, and use the Admin interface to create an Auction.

```bash
./minikube-run.sh
./k8s_connect_admin.sh 

```

```
admin > connect
Connected to cluster leader, node 1
admin > add-auction created-by=500 name=Tulips duration=1800
Auction added with id: 1
New auction: 'Tulips' (1)
Auction 1 is now in state OPEN. There have been 0 bids.
```

We want to demonstrate that we can replicate snapshots, so we will trigger a snapshot on the leader.

```bash
kubectl exec -it -n aeron-io-sample-cluster $(./k8s_find_leader.sh) -- /root/jar/snapshot.sh
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
snapshot has been retrieved.

```bash
kubectl logs -n aeron-io-sample-backup aeron-io-sample-backup-0
```

You should see log messages indicating that data has been copied.

```
Response from Cluster. Log Source Member: 2. Cluster Members: [0. 172.16.202.2:9003 (not leader), 1. 172.16.202.3:9103 (not leader), 2. 172.16.202.4:9203 (leader)]. Snapshots to retrieve: [Snapshot recordingId: 0, Snapshot recordingId: 1]
Updating log for recording [recordingId: 2, logPosition: 544, recordingId: 1, logPosition: 544, recordingId: 3, logPosition: -1]. Snapshots retrieved: [Snapshot recordingId: 1, Snapshot recordingId: 2]
Reached position 1408 in recording 3
```

## 2. Create a backup
Create a backup tarball of the `archive` and `cluster` directories on the Backup node,
and copy it out of the container.

```bash
kubectl exec -i -n aeron-io-sample-backup aeron-io-sample-backup-0 -- tar cvzf /root/jar/cluster_backup.tgz -C /root/jar/backup archive/ cluster/
kubectl cp -n aeron-io-sample-backup aeron-io-sample-backup-0:/root/jar/cluster_backup.tgz ./cluster_backup.tgz
```

## 3. Restart the system with clean state
Next, shutdown the containers and remove the volumes. Then, restart the containers in a fresh state.

```bash
./minikube-stop.sh
./minikube-run.sh
```

We can verify that the cluster doesn't have any state by listing the Auctions using the Admin interface.

```bash
./k8s_connect_admin.sh 
```

```
admin > connect
Connected to cluster leader, node 0
admin > list-auctions
No auctions exist in the cluster. Closed auctions are deleted automatically.
```

## 4. Restore from backup
Now, let's stop the containers, restore the data from the backup, and start the containers up again.

```bash
# Stop the application from writing data and remove the data directory
for i in {0..2}; do kubectl exec  -n aeron-io-sample-cluster aeron-io-sample-cluster-${i} -- sh -c 'kill -STOP $(pidof java); rm -r /root/jar/aeron-cluster/*'; done

# Replace the data directory from the backup
for i in {0..2}; do zcat cluster_backup.tgz | kubectl exec -i -n aeron-io-sample-cluster aeron-io-sample-cluster-${i} -- tar xf - -C /root/jar/aeron-cluster/; done

# Restart the Cluster nodes
kubectl rollout restart statefulset -n aeron-io-sample-cluster aeron-io-sample-cluster

# Wait for the Cluster restart to complete
kubectl rollout status statefulset -n aeron-io-sample-cluster aeron-io-sample-cluster
```

## 5. Verify the restore was successful
We can see that the data has been restored by again listing the Auctions with the Admin interface.

```bash
./k8s_connect_admin.sh 
```

```
admin > connect
Connected to cluster leader, node 0
admin > list-auctions
Auction count: 2
Auction 'Tulips' with id 1 created by 500 is now in state PRE_OPEN
Auction 'Daffodils' with id 2 created by 500 is now in state OPEN
```
