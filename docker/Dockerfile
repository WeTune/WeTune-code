FROM ubuntu:20.04
#FROM mcr.microsoft.com/mssql/server:2022-latest

LABEL maintainer="yangyicun@sjtu.edu.cn"
LABEL version="0.1"
LABEL description="WeTune Docker"


ARG DEBIAN_FRONTEND=noninteractive


RUN apt-get -y update && apt-get -y upgrade

# installing Java 17
RUN apt-get install -y openjdk-17-jdk openjdk-17-jre
#RUN apt-get install -y software-properties-common && \
#    add-apt-repository ppa:linuxuprising/java &&  \
#    apt-get update &&  \
#    apt-get install -y oracle-java17-installer oracle-java17-set-default \


# installing Gradle 7.3.3
RUN apt-get install -y wget unzip && \
    wget https://services.gradle.org/distributions/gradle-7.3.3-bin.zip -P /tmp && \
    unzip -d /opt/gradle /tmp/gradle-7.3.3-bin.zip
ENV GRADLE_HOME /opt/gradle/gradle-7.3.3
ENV PATH ${GRADLE_HOME}/bin:${PATH}


# installing Sql Server 2019
RUN apt-get install -y wget software-properties-common && \
    wget -qO- https://packages.microsoft.com/keys/microsoft.asc | apt-key add - && \
    add-apt-repository "$(wget -qO- https://packages.microsoft.com/config/ubuntu/20.04/mssql-server-2019.list)" && \
    apt-get update && \
    apt-get install -y mssql-server

# Sql Server setup: choose the 2nd edition of Sql Server; enter password: mssql2019Admin
#ENV MSSQL_SA_PASSWORD mssql2019Admin
#ENV MSSQL_PID developer
#RUN MSSQL_SA_PASSWORD=$MSSQL_SA_PASSWORD \
#    MSSQL_PID=$MSSQL_PID \
#    /opt/mssql/bin/mssql-conf -n setup accept-eula


EXPOSE 1433
## firewall setup
#RUN ufw enable && ufw allow 1433


# installing Sql Server command-line tools
RUN apt-get install -y curl && \
    curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add - && \
    curl https://packages.microsoft.com/config/ubuntu/20.04/prod.list | tee /etc/apt/sources.list.d/msprod.list && \
    apt-get update && \
    ACCEPT_EULA=Y apt-get install -y mssql-tools unixodbc-dev
ENV PATH /opt/mssql-tools/bin:$PATH

RUN apt-get install -y git-lfs && git lfs install

RUN rm -rf /var/lib/apt/lists/* && \
    apt-get clean

#CMD /bin/bash
CMD ["/usr/bin/tail", "-f", "/dev/null"]

