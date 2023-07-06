#!/bin/bash

echo "➡️  Stopping Admin Nodes..."
kubectl scale deployment -n aeron-io-sample-admin aeron-io-sample-admin --replicas=0

echo "➡️  Stopping Cluster Backup Nodes..."
kubectl scale statefulset -n aeron-io-sample-backup aeron-io-sample-backup --replicas=0

echo "➡️  Stopping Cluster Nodes..."
kubectl scale statefulset -n aeron-io-sample-cluster aeron-io-sample-cluster --replicas=0

echo "➡️  Removing Cluster Data..."
kubectl delete pvc -n aeron-io-sample-cluster data-volume-claim-aeron-io-sample-cluster-0
kubectl delete pvc -n aeron-io-sample-cluster data-volume-claim-aeron-io-sample-cluster-1
kubectl delete pvc -n aeron-io-sample-cluster data-volume-claim-aeron-io-sample-cluster-2

echo "➡️  Done"
