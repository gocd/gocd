#!/bin/sh

JAVAMAILSERVER_HOME=$PWD

if test "$1"; then
  JAVAMAILSERVER_CONF=$1
else
  JAVAMAILSERVER_CONF=$JAVAMAILSERVER_HOME
fi

LIBS="${JAVAMAILSERVER_HOME}/lib/*"
for i in $LIBS
do
  if [ "$LOCALCLASSPATH" != "" ]; then
    LOCALCLASSPATH=${LOCALCLASSPATH}:$i
  else
    LOCALCLASSPATH=$i
  fi
done

PID_FILE=mail-server.pid
CMD="java -cp $LOCALCLASSPATH com.ericdaugherty.mail.server.Mail $JAVAMAILSERVER_CONF"

eval "nohup $CMD &"
echo $! >$PID_FILE


