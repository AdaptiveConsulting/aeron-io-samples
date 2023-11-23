The steps to set up an EC2 instance and deploy Aeron Cluster to it:

1. Create and launch an EC2 instance.
2. Install Docker on the EC2 instance.
3. Configure EC2 Instance to be able to access ECR.
4. Create a Docker Compose configuration.
5. Run Aeron Cluster.

## 1. Create and launch EC2 instance

Please check out the [AWS Documentation](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/LaunchingAndUsingInstances.html) for general instructions on how to set up an EC2 instance.
For this exercise, please select the following options:
* **Application and OS Images (Amazon Machine Image)**: Linux Ubuntu Server 22.04 64-bit image
* **Instance type**: t2.micro
* **Key pair (login)**: create a new key pair, see [Documentation](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/create-key-pairs.html)
* **Network settings**: leave as default
* **Configure storage**: leave as default

Connect to your EC2 instance.

## 2. Install Docker on EC2 instance

Install the most recent Docker Engine package on your EC2 instance.

```sh
$ curl -fsSL https://get.docker.com -o get-docker.sh
$ sh get-docker.sh
```

Start the Docker service.

`sudo service docker start`

Add the *ubuntu* user (or, the user you are using to connect to your EC2 instance) to the docker group so you can execute Docker commands without using *sudo*

`sudo usermod -a -G docker ubuntu`

Test Docker installation

`docker run hello-world`

When successful it prints out information about docker starting with "Hello from Docker!".

## 3. Configure EC2 Instance to be able to access ECR

### 3a. Attach IAM Role that allows read-only access to ECR

Create a new AWS role either based on the pre-defined `AmazonEC2ContainerRegistryReadOnly` or one with the policy as below and attach to your EC2 instance
```
{
    "Version": "2023-05-05",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ecr:GetDownloadUrlForLayer",
                "ecr:BatchGetImage",
                "ecr:DescribeImages",
                "ecr:GetAuthorizationToken",
                "ecr:ListImages"
            ],
            "Resource": "*"
        }
    ]
}
```

### 3b. Install Amazon ECR Credential Helper

Unfortunately, Docker CLI does not support standard AWS authentication methods. Installing the Amazon ECR Credential Helper on the EC2 instance will allow pulling images from ECR without the need to use `docker login`:

`sudo apt install amazon-ecr-credential-helper`

Then create a file called *~/.docker/config.json* and paste the following content in:

```
{
	"credsStore": "ecr-login"
}
```


## 4. Create Docker Compose configuration

### 4a. Create `.env` file

The `.env` file is loaded by Docker Compose and allows you to change the image or tag without modifying `docker-compose.yaml`.

Create a file called `.env` and paste the following content in:

NOTE: The value of `IMAGE_TAG` will vary depending upon the version of the product available in the AWS Marketplace.
The exact value you should use is part of the Marketplace documentation. The `IMAGE_TAG` shown in this 
document is typically the latest AWS Marketplace version.

```shell
IMAGE_REGISTRY=709825985650.dkr.ecr.us-east-1.amazonaws.com/adaptive/aeron-premium
IMAGE_TAG=cluster-sample-v0.1.0-git7e38381
```

This file should be in the same directory as `docker-compose.yaml`

### 4b. Create `docker-compose.yaml` file

Create a file called `docker-compose.yaml` and paste the following content in:

<details>

<summary>Expand to see the docker-compose.yaml</summary>

#### docker-compose.yaml
```yaml
version: '3.8'

name: aeron

services:
  node0:
    image: ${IMAGE_REGISTRY}:${IMAGE_TAG}
    build:
      context: cluster
    hostname: cluster0
    shm_size: '1gb'
    networks:
      internal_bus:
        ipv4_address: 172.16.202.2
    environment:
      - CLUSTER_ADDRESSES=172.16.202.2,172.16.202.3,172.16.202.4
      - CLUSTER_NODE=0
      - CLUSTER_PORT_BASE=9000
  node1:
    image: ${IMAGE_REGISTRY}:${IMAGE_TAG}
    build:
      context: cluster
    hostname: cluster1
    shm_size: '1gb'
    networks:
      internal_bus:
        ipv4_address: 172.16.202.3
    environment:
      - CLUSTER_ADDRESSES=172.16.202.2,172.16.202.3,172.16.202.4
      - CLUSTER_NODE=1
      - CLUSTER_PORT_BASE=9000
  node2:
    image: ${IMAGE_REGISTRY}:${IMAGE_TAG}
    build:
      context: cluster
    hostname: cluster2
    shm_size: '1gb'
    networks:
      internal_bus:
        ipv4_address: 172.16.202.4
    environment:
      - CLUSTER_ADDRESSES=172.16.202.2,172.16.202.3,172.16.202.4
      - CLUSTER_NODE=2
      - CLUSTER_PORT_BASE=9000

networks:
  internal_bus:
    driver: bridge
    driver_opts:
      com.docker.network.bridge.enable_icc: 'true'
      com.docker.network.driver.mtu: 9000
      com.docker.network.enable_ipv6: 'false'
    ipam:
      driver: default
      config:
        - subnet: "172.16.202.0/24"
```
</details>

## 5. Running Aeron Cluster

Start the Aeron Cluster

`docker compose up -d`

To find the leader

`docker compose logs | grep LEADER`

Stop the cluster

`docker compose down`
