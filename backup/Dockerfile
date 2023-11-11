ARG REPO_NAME=docker.io/
ARG IMAGE_NAME=azul/zulu-openjdk-debian
ARG IMAGE_TAG=17
FROM ${REPO_NAME}${IMAGE_NAME}:${IMAGE_TAG}

SHELL [ "/bin/bash", "-o", "pipefail", "-c" ]

# Create aeron user and group
RUN groupadd -r aeron && useradd --no-log-init -r -g aeron aeron

# Create home directory for aeron
RUN mkdir /home/aeron && chown aeron:aeron /home/aeron
RUN mkdir /home/aeron/jar && chown aeron:aeron /home/aeron/jar

# Copy files and change ownership to aeron
COPY --chown=aeron:aeron --chmod=755 setup-docker.sh /home/aeron/dockerbuild/setup-docker.sh
RUN /home/aeron/dockerbuild/setup-docker.sh && rm --recursive --force "/home/aeron/dockerbuild"

# Grab a copy of aeron-all to run aeronstat and other tools
RUN apt-get install wget --quiet --assume-yes
RUN wget https://repo1.maven.org/maven2/io/aeron/aeron-all/1.42.1/aeron-all-1.42.1.jar -P /home/aeron/jar/
RUN apt-get remove wget --quiet --assume-yes
RUN apt-get clean --quiet --assume-yes

WORKDIR /home/aeron/jar/
COPY --chown=aeron:aeron --chmod=755 /build/libs/backup-uber.jar /home/aeron/jar/backup-uber.jar
COPY --chown=aeron:aeron --chmod=755 entrypoint.sh /home/aeron/jar/entrypoint.sh

# Set the user to aeron
USER aeron


ENTRYPOINT ["/home/aeron/jar/entrypoint.sh"]