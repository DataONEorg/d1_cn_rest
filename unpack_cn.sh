#!/bin/sh

TOMCAT7_WEBAPPS=/var/lib/tomcat7/webapps

service tomcat7 stop

rm -rf $TOMCAT7_WEBAPPS/cn $TOMCAT7_WEBAPPS/cn.war
 
cp /home/waltz/cn.war $TOMCAT7_WEBAPPS

unzip $TOMCAT7_WEBAPPS/cn.war -d $TOMCAT7_WEBAPPS/cn

chown -R tomcat7.tomcat7 $TOMCAT7_WEBAPPS/cn $TOMCAT7_WEBAPPS/cn.war

service tomcat7 start

tail -300f /var/lib/tomcat7/logs/catalina.out

