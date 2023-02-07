# Minikube

## Preparing the environment

Step 1:
- install Docker Desktop
- allocate at least 6 cores and 15GB of RAM to Docker Desktop

Step 2:
- install minikube, as appropriate for your OS and CPU following instructions here https://minikube.sigs.k8s.io/docs/start/

Run `./minikube-run.sh` to build, deploy and run the cluster and admin in minikube. 

## Running the sample

Step 1:
- DNS can be slow in Kubernetes, so run `kubectl logs aeron-io-sample-cluster-0 -n aeron-io-sample-cluster` until you see a log message mentioning `resolved name...`

Step 2: Connect to the cluster from admin
- Find the admin container's name with `kubectl get pods -n aeron-io-sample-admin`
- Connect to the admin container with `kubectl exec -it <POD NAME FROM ABOVE> -n aeron-io-sample-admin -- java --add-opens java.base/sun.nio.ch=ALL-UNNAMED -jar admin-uber.jar`
- `connect`