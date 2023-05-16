ARG REPO_NAME=docker.io/
ARG IMAGE_NAME=azul/zulu-openjdk-debian
ARG IMAGE_TAG=17
FROM ${REPO_NAME}${IMAGE_NAME}:${IMAGE_TAG}

SHELL [ "/bin/bash", "-o", "pipefail", "-c" ]

COPY --chmod=755 setup-docker.sh /root/dockerbuild/setup-docker.sh
COPY --chmod=755 setup-docker.sh /root/aeron/
RUN /root/dockerbuild/setup-docker.sh && rm --recursive --force "/root/dockerbuild"

WORKDIR /root/jar/
COPY --chmod=755 /build/libs/backup-uber.jar /root/jar/backup-uber.jar
COPY --chmod=755 entrypoint.sh /root/jar/entrypoint.sh
ENTRYPOINT ["/root/jar/entrypoint.sh"]