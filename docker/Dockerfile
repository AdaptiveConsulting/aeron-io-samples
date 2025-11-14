ARG REPO_NAME=docker.io/
ARG IMAGE_NAME=azul/zulu-openjdk-debian
ARG IMAGE_TAG=21
FROM ${REPO_NAME}${IMAGE_NAME}:${IMAGE_TAG} AS base

ARG AERON_VERSION=1.48.0

ENV DEBIAN_FRONTEND=noninteractive

SHELL [ "/bin/bash", "-o", "pipefail", "-c" ]

# Install tools
RUN apt-get --quiet --assume-yes update &&\
    apt-get --quiet --assume-yes upgrade &&\
    apt-get install --quiet --assume-yes --no-install-suggests\
    bash\
    less\
    procps\
    sysstat\
    && apt-get clean --quiet --assume-yes &&\
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

# Create aeron user and group and home directory
RUN groupadd -r aeron && useradd --no-log-init -r -g aeron aeron &&\
    mkdir -p /home/aeron/jar && chown -R aeron:aeron /home/aeron

ADD --chown=aeron:aeron --chmod=0644 "https://repo1.maven.org/maven2/io/aeron/aeron-all/${AERON_VERSION}/aeron-all-${AERON_VERSION}.jar" /home/aeron/jar/aeron-all-${AERON_VERSION}.jar

WORKDIR /home/aeron/jar/

# Set the user to aeron
USER aeron

ENTRYPOINT ["/home/aeron/jar/entrypoint.sh"]

FROM base AS admin

COPY --from=gradle --chown=aeron:aeron --chmod=0644 /admin/build/libs/*-uber.jar /home/aeron/jar/
COPY --from=gradle --chown=aeron:aeron --chmod=0755 /admin/*.sh /home/aeron/jar/

FROM base AS backup

COPY --from=gradle --chown=aeron:aeron --chmod=0644 /backup/build/libs/*-uber.jar /home/aeron/jar/
COPY --from=gradle --chown=aeron:aeron --chmod=0755 /backup/*.sh /home/aeron/jar/

FROM base AS cluster

COPY --from=gradle --chown=aeron:aeron --chmod=0644 /cluster/build/libs/*-uber.jar /home/aeron/jar/
COPY --from=gradle --chown=aeron:aeron --chmod=0755 /cluster/*.sh /home/aeron/jar/

FROM base AS standby

COPY --from=gradle --chown=aeron:aeron --chmod=0644 /standby/build/libs/*-uber.jar /home/aeron/jar/
COPY --from=gradle --chown=aeron:aeron --chmod=0755 /standby/*.sh /home/aeron/jar/

FROM cluster AS marketplace

COPY --from=gradle --chown=aeron:aeron --chmod=0644 /marketplace/build/libs/*-uber.jar /home/aeron/jar/
COPY --from=gradle --chown=aeron:aeron --chmod=0755 /marketplace/*.sh /home/aeron/jar/

ENTRYPOINT ["/home/aeron/jar/marektplace.sh"]
