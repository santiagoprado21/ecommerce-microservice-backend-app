FROM jenkins/jenkins:lts

USER root

# Instala JDK 17
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk curl gnupg2 software-properties-common lsb-release && \
    apt-get clean

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Instala Docker CLI
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - && \
    add-apt-repository \
    "deb [arch=amd64] https://download.docker.com/linux/debian \
    $(lsb_release -cs) stable" && \
    apt-get update && \
    apt-get install -y docker-ce-cli

RUN java -version && docker --version

# Agrega el usuario Jenkins al grupo docker
RUN usermod -aG docker jenkins

USER jenkins
