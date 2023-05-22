FROM tomcat:8.0
MAINTAINER josh_mccormack

COPY /target/FileConnector-2.0.3.war /usr/local/tomcat/webapps/