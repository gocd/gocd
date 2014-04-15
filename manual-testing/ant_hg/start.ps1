$HERE=$pwd
$CRUISE="$HERE\..\.."
set PRODUCTION_MODE=N
set DAEMON=Y
set JVM_DEBUG=Y
set SERVER_MEM=1024M
set SERVER_MAX_MEM=2048M

echo "Copying cruise config to target..."

$CONFIG_DIR="$CRUISE\target\cruise-server-2.0.0\config"
mkdir $CONFIG_DIR

cp cruise-config.xml backup_cruise_config.xml
ruby FixUrl.rb $HERE

cp cruise-config.xml "$CRUISE\target\cruise-server-2.0.0\config\cruise-config.xml"

cp backup_cruise_config.xml cruise-config.xml
rm backup_cruise_config.xml 

echo "Starting server..."
cd "$CRUISE\target\cruise-server-2.0.0"
cmd /c start start-server.bat

echo "Waiting 20 seconds for server to start..."
sleep 20

echo "Starting agent..."
cd "$CRUISE\target\cruise-agent-2.0.0"
cmd /c start start-agent.bat

echo "Done." 
 
