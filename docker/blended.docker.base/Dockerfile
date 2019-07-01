# The basic image to run Blended containers
FROM alpine:3.10

MAINTAINER Andreas Gies 

ENV USER=blended
ENV UID=500
ENV GID=500

ENV JAVA_URL=https://cdn.azul.com/zulu/bin/zulu8.38.0.13-ca-jdk8.0.212-linux_musl_x64.tar.gz

# Installation section

RUN apk --no-cache add curl git openssh-client

RUN addgroup -g "${GID}" "${USER}" \
 && adduser \
  --disabled-password \
  --gecos "" \
  --home "$(pwd)" \
  --ingroup "$USER" \
  --no-create-home \
  --uid "$UID" \
  "$USER"

ADD files/profile.d /etc/profile.d  

RUN mkdir -p /opt \
 && curl ${JAVA_URL} | tar -xzC /opt \
 && ln -s $(ls -d /opt/zulu*) /opt/java 

# End of Installation section
