#!/bin/bash

HERE=$PWD
CRUISE=$HERE/../../..
export PRODUCTION_MODE=N
export DAEMON=Y
export JVM_DEBUG=Y
export SERVER_MEM=1024M
export SERVER_MAX_MEM=2048M

echo "Copying cruise config to target..."

CONFIG_DIR=$CRUISE/target/cruise-server-2.0.0/config
if [ ! -d $CONFIG_DIR ]; then
  mkdir $CONFIG_DIR
fi

cp shine-cruise-config.xml $CRUISE/target/cruise-server-2.0.0/config/cruise-config.xml

echo "Deleting current Repo"
./clear_repo.sh
echo "Creating a new repo at Dummy"
./create_repo.sh

echo "Starting server..."
cd $CRUISE/target/cruise-server-2.0.0
bash server.sh > server.log & 

echo "Waiting 20 seconds for server to start..."
sleep 20

echo "Starting agent..."
cd $CRUISE/target/cruise-agent-2.0.0
bash agent.sh > agent.log & 

echo "Done." 
 
