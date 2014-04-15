#!/bin/bash

HERE=$PWD
CRUISE=$HERE/../..
export PRODUCTION_MODE=N
export DAEMON=Y
export JVM_DEBUG=Y
export SERVER_MEM=1024M
export SERVER_MAX_MEM=2048M
export SHINE_USERNAME=root
export SHINE_PASSWORD=badger
export YOURKIT_DISABLE_TRACING=Y

echo "Copying cruise config to target..."
VERSION=12.4.0
CONFIG_DIR=$CRUISE/target/go-server-$VERSION/config
if [ ! -d $CONFIG_DIR ]; then
  mkdir $CONFIG_DIR
fi

cp cruise-config.xml backup_cruise_config.xml
ruby FixUrl.rb `pwd`

cp cruise-config.xml $CRUISE/target/go-server-$VERSION/config/cruise-config.xml

cp backup_cruise_config.xml cruise-config.xml
rm backup_cruise_config.xml 

echo "Starting server..."
cd $CRUISE/target/go-server-$VERSION
bash server.sh > server.log &

echo "Waiting 20 seconds for server to start..."
sleep 20

echo "Starting agent..."
cd $CRUISE/target/go-agent-$VERSION
bash agent.sh > agent.log &

echo "Done." 
 
