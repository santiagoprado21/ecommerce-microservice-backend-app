FROM jenkins/jenkins:lts

USER root

# Instala JDK 17 y dependencias necesarias
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk curl gnupg2 ca-certificates lsb-release && \
    apt-get clean

# Configura el repositorio oficial de Docker (sin apt-key)
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list && \
    apt-get update && \
    apt-get install -y docker-ce-cli

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN java -version

USER jenkins
