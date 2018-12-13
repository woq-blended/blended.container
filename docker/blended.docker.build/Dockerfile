# The basic image to run Blended container builds from Jenkins pipelines
# Jenkins expects a jenkins account with user/pw: jenkins/jenkins which must have the same
# UID/GID as on the host, therefore we set the UID/GID via ARG

FROM debian:latest

MAINTAINER Andreas Gies 

ARG USER=tcagent
ARG GROUP=tcagent

ARG JAVA_HOME=/opt/java
ARG M2_HOME=/opt/maven
ARG SBT_HOME=/opt/sbt

ARG TC_URL=http://http://35.246.227.160
ARG JAVA_URL=https://download.oracle.com/otn-pub/java/jdk/8u191-b12/2787e4a523244c269598db4e85c51e0c/jdk-8u191-linux-x64.tar.gz
ARG SBT_URL=https://piccolo.link/sbt-1.2.7.tgz

# Installation section
ENV JAVA_HOME=${JAVA_HOME} \
    M2_HOME=${M2_HOME} \
    SBT_HOME=${SBT_HOME} \
    PATH=${JAVA_HOME}/bin:${M2_HOME}/bin:${SBT_HOME}/bin:${HOME}/node_modules/.bin:$PATH

RUN groupadd ${GROUP} ; useradd -d /home/${USER} -s /bin/bash -g ${GROUP} -m ${USER} \
 && apt-get -y -q update \
 && apt-get -y -q install gnupg2 apt-transport-https

RUN apt-get -y -q update \
 && apt-get -y -q install \
    curl \
    unzip \
    gnupg2 \
    openssh-server \
    wget \
    graphviz \
    git \ 
    chromium \
    vim \
    chromium-l10n

RUN cd /opt \
 && wget -q -c -O - --header "Cookie: oraclelicense=accept-securebackup-cookie" ${JAVA_URL} | tar -xvzf - \
 && wget wget -q -c http://35.246.227.160/update/buildAgent.zip ; unzip -d tcagent buildAgent.zip ; rm buildAgent.zip \
 && wget -q -c -O - ${SBT_URL} | tar -xvzf - \
 && wget -q -c -O - http://ftp.fau.de/apache/maven/maven-3/3.6.0/binaries/apache-maven-3.6.0-bin.tar.gz | tar -xvzf - \
 && sed -i 's|session required pam_loginuid.so|session optional pam_loginuid.so|g' /etc/pam.d/sshd \
 && mkdir -p /var/run/sshd

ADD files/buildAgent.properties /opt/tcagent/conf/buildAgent.properties 

RUN ln -s /opt/jdk1.8.0_191 /opt/java \
 && ln -s /opt/apache-maven-3.6.0 /opt/maven \
 && chown -R ${USER}:${GROUP} /opt/tcagent \
 && echo "${USER}:${GROUP}" | chpasswd

RUN wget -q -c -O - https://raw.githubusercontent.com/creationix/nvm/v0.33.1/install.sh | /bin/bash; /bin/bash -l -c "source $HOME/.nvm/nvm.sh ; nvm install 8.9.1 ; npm install yarn" 

USER ${USER}

# Jenkins needs access to an open SSH port
EXPOSE 22

CMD ["/usr/sbin/sshd", "-D"]

