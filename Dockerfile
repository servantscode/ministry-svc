#Dockerfile

FROM servantcode/tomcat-elk-logging

LABEL maintainer="greg@servantscode.org"

COPY ./build/libs/ministry-svc-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
