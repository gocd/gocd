
cd `hg root`
ROOT=`pwd`

cd $ROOT/target/cruise-agent-*
AGENT_HOME=`pwd`

cp $ROOT/target/agent/agent.jar $AGENT_HOME

for i in $(seq 1 10) 
do
  rm -rf $AGENT_HOME/agent-$i
  mkdir $AGENT_HOME/agent-$i
  cd $AGENT_HOME/agent-$i
  nohup java -jar ../agent.jar https://localhost:8154/cruise &2>1 >agent.log &  
done

