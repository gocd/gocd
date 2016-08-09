#!/bin/bash
##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
##########################################################################

SERVICE_NAME=${1:-go-agent}
PRODUCTION_MODE=${PRODUCTION_MODE:-"Y"}

if [ "$PRODUCTION_MODE" == "Y" ]; then
    if [ -f /etc/default/${SERVICE_NAME} ]; then
        echo "[$(date)] using default settings from /etc/default/${SERVICE_NAME}"
        . /etc/default/${SERVICE_NAME}
    fi
fi

yell() {
  echo "WARN: $*" >&2;
}

die() {
    yell "FATAL: $1"
    exit ${2:-1}
}

function autoDetectJavaExecutable() {
  local java_cmd
  # Prefer using GO_JAVA_HOME, over JAVA_HOME
  GO_JAVA_HOME=${GO_JAVA_HOME:-"$JAVA_HOME"}

  if [ -n "$GO_JAVA_HOME" ] ; then
      if [ -x "$GO_JAVA_HOME/jre/sh/java" ] ; then
          # IBM's JDK on AIX uses strange locations for the executables
          java_cmd="$GO_JAVA_HOME/jre/sh/java"
      else
          java_cmd="$GO_JAVA_HOME/bin/java"
      fi
      if [ ! -x "$java_cmd" ] ; then
          die "ERROR: GO_JAVA_HOME is set to an invalid directory: $GO_JAVA_HOME

Please set the GO_JAVA_HOME variable in your environment to match the
location of your Java installation."
      fi
  else
      java_cmd="java"
      command -v java >/dev/null 2>&1 || die "ERROR: GO_JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the GO_JAVA_HOME variable in your environment to match the
location of your Java installation."
  fi

  echo "$java_cmd"
}

declare -a _stringToArgs
function stringToArgsArray() {
  _stringToArgs=("$@")
}

function autoDetectGoServerUrl() {
  local url

  if [[ ! -z "${GO_SERVER}" || ! -z "${GO_SERVER_PORT}" ]]; then
    yell "The environment variable GO_SERVER and GO_SERVER_PORT has been deprecated in favor of GO_SERVER_URL. Please set GO_SERVER_URL instead to a https url (https://example.com:8154/go)"
  fi

  if [ -z "${GO_SERVER_URL}" ]; then
    if [ -z "${GO_SERVER}" ]; then
      url="https://127.0.0.1:8154/go"
    else
      url="https://${GO_SERVER}:8154/go"
    fi
  else
    url="${GO_SERVER_URL}"
  fi

  echo "${url}"
}

CWD="$(dirname "$0")"
AGENT_DIR="$(cd "$CWD" && pwd)"

AGENT_MEM=${AGENT_MEM:-"128m"}
AGENT_MAX_MEM=${AGENT_MAX_MEM:-"256m"}
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

STDOUT_LOG_FILE=$LOG_DIR/${SERVICE_NAME}-bootstrapper.out.log

if [ "$PID_FILE" ]; then
    echo "[$(date)] Use PID_FILE: $PID_FILE"
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
    echo "[$(date)] Starting up VNC on :3"
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

eval stringToArgsArray "$AGENT_BOOTSTRAPPER_ARGS"
AGENT_BOOTSTRAPPER_ARGS=("${_stringToArgs[@]}")

RUN_CMD=("$(autoDetectJavaExecutable)" "-jar" "$AGENT_DIR/agent-bootstrapper.jar" "-serverUrl" "$(autoDetectGoServerUrl)" "${AGENT_BOOTSTRAPPER_ARGS[@]}")

echo "[$(date)] Starting Go Agent Bootstrapper with command: ${RUN_CMD[@]}" >>"$STDOUT_LOG_FILE"
echo "[$(date)] Starting Go Agent Bootstrapper in directory: $AGENT_WORK_DIR" >>"$STDOUT_LOG_FILE"
echo "[$(date)] AGENT_STARTUP_ARGS=$AGENT_STARTUP_ARGS" >>"$STDOUT_LOG_FILE"
cd "$AGENT_WORK_DIR"

if [ "$DAEMON" == "Y" ]; then
    exec nohup "${RUN_CMD[@]}" >> "$STDOUT_LOG_FILE" 2>&1 &
    disown $!
    echo $! >"$PID_FILE"
else
    exec "${RUN_CMD[@]}"
fi
