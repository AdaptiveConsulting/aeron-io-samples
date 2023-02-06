# Docker Desktop on Mac

Step 1:
- install Docker Desktop
- allocate at least 6 cores and 15GB of RAM to Docker Desktop

Step 2:
- install minikube, appropriate for your OS and CPU following instructions here https://minikube.sigs.k8s.io/docs/start/

Step 3:
- build the docker images for Admin and Cluster
- within `/cluster` folder: `docker build -t cluster:latest .`
- within `/admin` folder: `docker build -t admin:latest .`

Step 4:
- start the minikube cluster. Note that the specific version of Kubernetes is selected to align with cloud providers such as AWS EKS
- `minikube start --kubernetes-version="v1.24.3" --driver="docker" --memory="15G" --cpus="6" --addons="registry" --embed-certs="true"`

Step 5:
- load the docker images into minikube: `minikube image load cluster:latest` and `minikube image load admin:latest`

Step 6:
- Apply the helm scripts to deploy the cluster and admin: `kubectl apply -f .`

The cluster and admin should now be running within the `aeron-io-sample` namespace.
You can check the status of the pods with `kubectl get pods -n aeron-io-sample` and the logs with `kubectl logs aeron-io-sample-1 -n aeron-io-sample` (using cluster node 1 as the example).