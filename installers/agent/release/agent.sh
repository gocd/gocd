#!/bin/bash
#*************************GO-LICENSE-START********************************
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#*************************GO-LICENSE-END**********************************


SERVICE_NAME=${1:-go-agent}
PRODUCTION_MODE=${PRODUCTION_MODE:-"Y"}

if [ "$PRODUCTION_MODE" == "Y" ]; then
    if [ -f /etc/default/${SERVICE_NAME} ]; then
        echo "[`date`] using default settings from /etc/default/${SERVICE_NAME}"
        . /etc/default/${SERVICE_NAME}
    fi
fi

CWD=`dirname "$0"`
AGENT_DIR=`(cd "$CWD" && pwd)`

AGENT_MEM=${AGENT_MEM:-"128m"}
AGENT_MAX_MEM=${AGENT_MAX_MEM:-"256m"}
GO_SERVER=${GO_SERVER:-"127.0.0.1"}
GO_SERVER_PORT=${GO_SERVER_PORT:-"8153"}
JVM_DEBUG_PORT=${JVM_DEBUG_PORT:-"5006"}
VNC=${VNC:-"N"}


#If this script is launched to start testing agent by production agent while running twist test, the variable
# AGENT_WORK_DIR is already set by the production agent. But testing agent should not use that.

if [ "$PRODUCTION_MODE" == "Y" ]; then
    AGENT_WORK_DIR=${AGENT_WORK_DIR:-"$AGENT_DIR"}
else
    AGENT_WORK_DIR=$AGENT_DIR
fi

if [ ! -d "${AGENT_WORK_DIR}" ]; then
    echo Agent working directory ${AGENT_WORK_DIR} does not exist
    exit 2
fi

if [ "$PRODUCTION_MODE" == "Y" ]; then
    if [ -d /var/log/${SERVICE_NAME} ]; then
        LOG_DIR=/var/log/${SERVICE_NAME}
    else
	LOG_DIR=$AGENT_WORK_DIR
    fi
else
    LOG_DIR=$AGENT_WORK_DIR
fi

LOG_FILE=$LOG_DIR/${SERVICE_NAME}-bootstrapper.log

if [ "$PID_FILE" ]; then
    echo "[`date`] Use PID_FILE: $PID_FILE"
elif [ "$PRODUCTION_MODE" == "Y" ]; then
    if [ -d /var/run/go-agent ]; then
        PID_FILE=/var/run/go-agent/${SERVICE_NAME}.pid
    else
    	PID_FILE="$AGENT_WORK_DIR/go-agent.pid"
    fi
else
    PID_FILE="$AGENT_WORK_DIR/go-agent.pid"
fi

if [ "$VNC" == "Y" ]; then
    echo "[`date`] Starting up VNC on :3"
    /usr/bin/vncserver :3
    DISPLAY=:3
    export DISPLAY
fi

if [ "$JVM_DEBUG" != "" ]; then
    JVM_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${JVM_DEBUG_PORT}"
else
    JVM_DEBUG=""
fi

if [ "$GC_LOG" != "" ]; then
    GC_LOG="-verbose:gc -Xloggc:${SERVICE_NAME}-gc.log -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGC"
else
    GC_LOG=""
fi

AGENT_STARTUP_ARGS="-Dcruise.console.publish.interval=10 -Xms$AGENT_MEM -Xmx$AGENT_MAX_MEM $JVM_DEBUG $GC_LOG $GO_AGENT_SYSTEM_PROPERTIES"
if [ "$TMPDIR" != "" ]; then
    AGENT_STARTUP_ARGS="$AGENT_STARTUP_ARGS -Djava.io.tmpdir=$TMPDIR"
fi
if [ "$USE_URANDOM" != "false" ] && [ -e "/dev/urandom" ]; then
    AGENT_STARTUP_ARGS="$AGENT_STARTUP_ARGS -Djava.security.egd=file:/dev/./urandom"
fi
export AGENT_STARTUP_ARGS
export LOG_DIR
export LOG_FILE

CMD="$JAVA_HOME/bin/java -jar \"$AGENT_DIR/agent-bootstrapper.jar\" $GO_SERVER $GO_SERVER_PORT"

echo "[`date`] Starting Go Agent Bootstrapper with command: $CMD" >>"$LOG_FILE"
echo "[`date`] Starting Go Agent Bootstrapper in directory: $AGENT_WORK_DIR" >>"$LOG_FILE"
echo "[`date`] AGENT_STARTUP_ARGS=$AGENT_STARTUP_ARGS" >>"$LOG_FILE"
cd "$AGENT_WORK_DIR"

if [ "$JAVA_HOME" == "" ]; then
    echo "Please set JAVA_HOME to proceed."
    exit 1
fi

if [ "$DAEMON" == "Y" ]; then
    eval exec nohup "$CMD" >>"$LOG_FILE" 2>&1 &
    echo $! >"$PID_FILE"
else
    eval exec "$CMD"
fi
