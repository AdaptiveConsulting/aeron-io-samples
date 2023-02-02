# Aeron.io Samples

A simple cluster and client built using Aeron Cluster.

# Running

Running the samples in Docker is the easiest way to get started. See the [docker readme](docker/readme.md) for more details.

## Local

- run `./gradlew` to build the code
- in one terminal, run `./gradlew runSingleNodeCluster`
- in another terminal, run the admin application. See [admin readme](admin/readme.md) for more details.

Alternatively, you can also run the samples in your local environment.

# Development requirements

Using the Adaptive onboarding script is not required, but will be helpful.

- Java 17 (Zulu 17.0.6 was used for development)
- Gradle 7.6

# Runtime requirements

- Linux/macOS (if you want to run the samples in your local environment; windows untested)
- Docker Compose 2.x (if you want to run the samples in Docker)
- (coming soon) Kubernetes 1.24+ (if you want to run the samples in Kubernetes)