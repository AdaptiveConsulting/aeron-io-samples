# Docker Desktop on Mac

Step 0:
- build everything with `./gradlew build`

Step 1:
- install Docker Desktop
- allocate at least 6 cores and 15GB of RAM to Docker Desktop

Step 2:
- install minikube, appropriate for your OS and CPU following instructions here https://minikube.sigs.k8s.io/docs/start/

Step 3:
- build the docker images for Admin and Cluster using either `./build-container-images.sh` or the following commands:
- within `/cluster` folder: `docker build -t cluster:latest . --no-cache`
- within `/admin` folder: `docker build -t admin:latest . --no-cache`

Step 4:
- start the minikube cluster. Note that the specific version of Kubernetes is selected to align with cloud providers such as AWS EKS
- `minikube start --kubernetes-version="v1.24.3" --driver="docker" --memory="15G" --cpus="6" --addons="registry" --embed-certs="true"`

Step 5:
- load the docker images into minikube: `minikube image load cluster:latest` and `minikube image load admin:latest`

Step 6:
- Apply the kubernetes scripts to deploy the cluster and admin by running  `kubectl apply -f .` in both the cluster and admin folders.

The cluster and admin should now be running within the `aeron-io-sample-cluster` and `aeron-io-sample-admin` namespaces respectively.
You can check the status of the pods with `kubectl get pods -n aeron-io-sample-cluster` and `kubectl get pods -n aeron-io-sample-admin` and the logs with `kubectl logs aeron-io-sample-cluster-1 -n aeron-io-sample-cluster` (using cluster node 1 as the example).

Step 7:
- Find the admin container's name with `kubectl get pods -n aeron-io-sample-admin`
- Connect to the admin container with `kubectl exec -it <POD NAME FROM ABOVE> -n aeron-io-sample-admin -- /bin/bash`
- Run admin with `java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar admin-uber.jar`

Step 8: connect to the cluster from the admin container
- getting the admin container internal IP: `kubectl describe pod aeron-io-sample-admin -n aeron-io-sample-admin` -- see `IP:`
- getting the admin container external IP: from within the admin container, run `env | grep AERON_IO_SAMPLE_ADMIN_SERVICE_HOST`

none of these will work (note the IPs are dynamic and you will need to look them up; `kubectl get endpoints -n aeron-io-sample-admin` helps):

- `connect hostnames=aeron-io-sample-cluster-0.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local.,aeron-io-sample-cluster-1.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local.,aeron-io-sample-cluster-2.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local. thishost=aeron-io-sample-admin.aeron-io-sample-admin.svc.cluster.local.`
- `connect hostnames=aeron-io-sample-cluster-0.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local.,aeron-io-sample-cluster-1.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local.,aeron-io-sample-cluster-2.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local. thishost=10.106.232.139`
- `connect hostnames=aeron-io-sample-cluster-0.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local.,aeron-io-sample-cluster-1.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local.,aeron-io-sample-cluster-2.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local. thishost=10.244.0.8`
- `connect hostnames=aeron-io-sample-cluster-0.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local.,aeron-io-sample-cluster-1.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local.,aeron-io-sample-cluster-2.aeron-io-sample-cluster.aeron-io-sample-cluster.svc.cluster.local. thishost=aeron-io-sample-admin-5bdb9c6c49-jfsl9.aeron-io-sample-admin.svc.cluster.local.`

Either binding fails, or the cluster nodes can't connect to the admin container.