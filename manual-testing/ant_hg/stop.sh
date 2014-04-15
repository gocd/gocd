#!/bin/bash

HERE=$PWD
CRUISE=../../
export PRODUCTION_MODE=N
export DAEMON=Y

VERSION=12.4.0

echo "Stopping server..."
cd $CRUISE/target/go-server-$VERSION
bash stop-server.sh

echo "Stopping agent..."
cd $CRUISE/target/go-agent-$VERSION
bash stop-agent.sh  

echo "Done." 
 
