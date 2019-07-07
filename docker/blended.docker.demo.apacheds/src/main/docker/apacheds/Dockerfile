FROM atooni/blended-base:latest
MAINTAINER Blended Team version: 2.4.0-RC6-SNAPSHOT

ENV JAVA_HOME /opt/java
ENV APACHEDS_VERSION 2.0.0.AM25
ENV APACHEDS_URL http://archive.apache.org/dist/directory/apacheds/dist/${APACHEDS_VERSION}/apacheds-${APACHEDS_VERSION}.tar.gz

RUN apk --no-cache add openldap-clients gettext vim bash

RUN curl ${APACHEDS_URL} | tar -xzC /opt \
 && ln -s $(ls -d /opt/apacheds*) /opt/apacheds

ADD files /opt/apacheds

ENTRYPOINT ["/bin/bash", "-l", "/opt/apacheds/scripts/start.sh"]

EXPOSE 10389

