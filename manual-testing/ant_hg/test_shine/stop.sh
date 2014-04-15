#!/bin/bash

HERE=$PWD
CRUISE=$HERE/../../..
export PRODUCTION_MODE=N
export DAEMON=Y

echo "Stopping server..."
cd $CRUISE/target/cruise-server-2.0.0
bash stop-server.sh

echo "Stopping agent..."
cd $CRUISE/target/cruise-agent-2.0.0
bash stop-agent.sh  

echo "Done." 
 
