#!/bin/bash

$HERE=$pwd
$CRUISE="$HERE/../../"
set PRODUCTION_MODE=N
set DAEMON=Y

echo "Stopping server..."
cd "$CRUISE/target/cruise-server-2.0.0"
cmd /c .\stop-server.bat

echo "Stopping agent..."
cd "$CRUISE/target/cruise-agent-2.0.0"
cmd /c .\stop-agent.bat

echo "Done." 
 
