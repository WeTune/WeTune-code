FROM ubuntu:20.04
LABEL authors="jz"

RUN apt-get -y update && apt-get -y upgrade

# installing Java 17
RUN apt-get install -y openjdk-17-jdk openjdk-17-jre

# installing Gradle 7.3.3
RUN apt-get install -y wget unzip && \
    wget https://services.gradle.org/distributions/gradle-7.3.3-bin.zip -P /tmp && \
    unzip -d /opt/gradle /tmp/gradle-7.3.3-bin.zip
ENV GRADLE_HOME /opt/gradle/gradle-7.3.3
ENV PATH ${GRADLE_HOME}/bin:${PATH}

#CMD /bin/bash
CMD ["/usr/bin/tail", "-f", "/dev/null"]
