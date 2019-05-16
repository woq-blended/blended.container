# The basic image to run Blended containers
FROM centos:centos7

MAINTAINER Andreas Gies version: 2.0-SNAPSHOT

# Installation section
RUN groupadd blended
RUN useradd -d /home/blended -s /bin/bash -g blended -m blended

# ADD https://cdn.azul.com/zulu/bin/zulu8.33.0.1-jdk8.0.192-linux_x64.tar.gz /opt
ADD files/zulu8.33.0.1-jdk8.0.192-linux_x64.tar.gz /opt
ADD files/profile.d /etc/profile.d
RUN ln -s /opt/zulu8.33.0.1-jdk8.0.192-linux_x64 /opt/java && yum update -y && yum install -y glibc.i686 

# End of Installation section
