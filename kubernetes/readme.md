# Kubernetes

These assume that you are running either on an Intel or Apple Silicon Mac with Docker Desktop and Kubernetes enabled or minikube installed, or on a Linux machine with minikube installed.

## Setup Kubernetes

### MiniKube



Step 1:
- install Docker Desktop
- allocate at least 6 cores and 15GB of RAM to Docker Desktop

Step 2:
- install minikube, as appropriate for your OS and CPU following instructions here https://minikube.sigs.k8s.io/docs/start/

Step 3:
- install the correct version of Kubernetes: `minikube start --kubernetes-version="v1.24.3" --driver="docker" --memory="15G" --cpus="6" --addons="registry" --embed-certs="true"`

Run `./minikube-run.sh` to build, deploy and run the cluster and admin. 

### Docker Desktop with Kubernetes enabled

> **Note**: tested with Kubernetes `1.24.5` - there is no way to override the version of Kubernetes used by Docker Desktop. Minikube is recommended.

As with minikube, allocate at least 6 cores and 15GB of RAM to Docker Desktop.

Run `./docker-desktop-k8s-run.sh` to build, deploy and run the cluster and admin.

## Running the sample

Step 1:
- DNS can be slow in Kubernetes, so run `kubectl logs aeron-io-sample-cluster-0 -n aeron-io-sample-cluster` until you see a log message mentioning `resolved name...`

Step 2: Connect to the cluster from admin
- Find the admin container's name with `kubectl get pods -n aeron-io-sample-admin`
- Connect to the admin container with `kubectl exec -it <POD NAME FROM ABOVE> -n aeron-io-sample-admin -- java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar admin-uber.jar`
- `connect`

Step 3: interact with the cluster. See the [Admin](../admin/readme.md) for more details.