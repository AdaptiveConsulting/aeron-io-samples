# Aeron.io Quick Start

> **Note**: This code in this repo is for demonstration purposes only and is not representative of a production system. Please contact info@aeron.io for help configuring your system.

A Cluster and command line client built using Aeron Cluster.

![demo](images/docker_demo.gif)

# Running

Running the samples in Docker is the easiest way to get started. See the [docker readme](docker/readme.md) for more details.

## Local

-   run `./gradlew` to build the code
-   in one terminal, run `./gradlew runSingleNodeCluster`
-   in another terminal, run the admin application. See [admin readme](admin/readme.md) for more details.

# Development requirements

-   Java 21
-   Gradle 8.14.2

# Runtime requirements

-   Linux/macOS (if you want to run the samples in your local environment; windows currently untested)
-   Docker Compose 2.x - see [docker readme](docker/readme.md) for more details
-   Kubernetes 1.32.x - see [kubernetes readme](kubernetes/readme.md) for more details
-   Minikube 1.36.x - if running Kubernetes with minikube. See [kubernetes readme](kubernetes/readme.md) for more details
