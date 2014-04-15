#!/bin/sh

JES_HOME="conf.run"

CMD="java -client -Ddns.server= -Ddns.search= -Dsun.net.spi.nameservice.provider.1=dns,dnsjava -Djava.security.policy=$JES_HOME/jes.policy -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dlog4j.configuration=file:$JES_HOME/conf/log4j.properties -cp jes.jar:lib/log4j-1.2.15.jar com.ericdaugherty.mail.server.Mail conf.run"

PID_FILE=mail-server.pid

eval "nohup $CMD &"
echo $! >$PID_FILE


