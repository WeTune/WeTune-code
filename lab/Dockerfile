FROM --platform=linux/amd64 ubuntu:20.04
LABEL authors="jz"

RUN apt-get -y update && apt-get -y upgrade

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get install -y mysql-server

RUN apt-get install -y openssh-server

# installing Java 17
RUN apt-get install -y openjdk-17-jdk openjdk-17-jre

# installing Gradle 7.3.3
RUN apt-get install -y wget unzip && \
    wget https://services.gradle.org/distributions/gradle-7.3.3-bin.zip -P /tmp && \
    unzip -d /opt/gradle /tmp/gradle-7.3.3-bin.zip
ENV GRADLE_HOME /opt/gradle/gradle-7.3.3
ENV PATH ${GRADLE_HOME}/bin:${PATH}

# cloning WeTune
RUN apt-get install -y git

RUN apt-get install dos2unix

COPY startup.sh /app/
RUN dos2unix /app/startup.sh
RUN chmod +x /app/startup.sh
CMD bash /app/startup.sh && \
    /usr/bin/tail -f /dev/null
