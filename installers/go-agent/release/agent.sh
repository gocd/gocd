#!/bin/bash
##########################################################################
# Copyright 2019 ThoughtWorks, Inc.
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
# by default we do not daemonize
DAEMON="${DAEMON:-N}"
SERVICE_NAME=${1:-go-agent}

if [ "$2" == "service_mode" ]; then
  if [ -f /etc/default/${SERVICE_NAME} ]; then
    . /etc/default/${SERVICE_NAME}
  fi

  # no point in not daemonizing the service
  DAEMON=Y
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

  if [ -z "${GO_SERVER_URL}" ]; then
      url="https://127.0.0.1:8154/go"
  else
    url="${GO_SERVER_URL}"
  fi

  echo "${url}"
}

CWD="$(dirname "$0")"
AGENT_DIR="$(cd "$CWD" && pwd)"

AGENT_MEM=${AGENT_MEM:-"128m"}
AGENT_MAX_MEM=${AGENT_MAX_MEM:-"256m"}
AGENT_WORK_DIR="${AGENT_WORK_DIR:-$AGENT_DIR}"


if [ ! -d "${AGENT_WORK_DIR}" ]; then
  echo Agent working directory ${AGENT_WORK_DIR} does not exist
  exit 2
fi

if [ "$2" == 'service_mode' ] && [ -d "/var/log/${SERVICE_NAME}" ]; then
  GO_AGENT_LOG_DIR="/var/log/${SERVICE_NAME}"
else
  GO_AGENT_LOG_DIR="$AGENT_WORK_DIR/logs"
  mkdir -p "${GO_AGENT_LOG_DIR}"
fi

STDOUT_LOG_FILE="$GO_AGENT_LOG_DIR/${SERVICE_NAME}-bootstrapper.out.log"

if [ "$2" == "service_mode" ] && [ -d "/var/run/go-agent" ]; then
  PID_FILE="/var/run/go-agent/${SERVICE_NAME}.pid"
else
  PID_FILE="$AGENT_WORK_DIR/go-agent.pid"
fi

if [ ! -z ${VNC+x} ]; then
  yell "Support for the variable VNC has been removed. Please see https://github.com/gocd/gocd/issues/4005#issuecomment-348138015 for alternatives."
fi

AGENT_STARTUP_ARGS="-Dcruise.console.publish.interval=10 -Xms$AGENT_MEM -Xmx$AGENT_MAX_MEM $GO_AGENT_SYSTEM_PROPERTIES"

if [ "$2" == "service_mode" ]; then
  AGENT_STARTUP_ARGS="$AGENT_STARTUP_ARGS -Dgocd.agent.log.dir=$GO_AGENT_LOG_DIR"
fi

if [ "$TMPDIR" != "" ]; then
    AGENT_STARTUP_ARGS="$AGENT_STARTUP_ARGS -Djava.io.tmpdir=$TMPDIR"
fi
if [ "$USE_URANDOM" != "false" ] && [ -e "/dev/urandom" ]; then
    AGENT_STARTUP_ARGS="$AGENT_STARTUP_ARGS -Djava.security.egd=file:/dev/./urandom"
fi
export AGENT_STARTUP_ARGS

eval stringToArgsArray "$AGENT_BOOTSTRAPPER_ARGS"
AGENT_BOOTSTRAPPER_ARGS=("${_stringToArgs[@]}")

eval stringToArgsArray "$AGENT_BOOTSTRAPPER_JVM_ARGS"
AGENT_BOOTSTRAPPER_JVM_ARGS=("-Dgocd.agent.log.dir=$GO_AGENT_LOG_DIR")
if [ "$DAEMON" == "Y" ]; then
  AGENT_BOOTSTRAPPER_JVM_ARGS+=("-Dgocd.redirect.stdout.to.file=$STDOUT_LOG_FILE")
fi

AGENT_BOOTSTRAPPER_JVM_ARGS+=("${_stringToArgs[@]}")

RUN_CMD=("$(autoDetectJavaExecutable)" "${AGENT_BOOTSTRAPPER_JVM_ARGS[@]}" "-jar" "$AGENT_DIR/agent-bootstrapper.jar" "-serverUrl" "$(autoDetectGoServerUrl)" "${AGENT_BOOTSTRAPPER_ARGS[@]}")

cd "$AGENT_WORK_DIR"

if [ "$DAEMON" == "Y" ]; then
    exec nohup "${RUN_CMD[@]}" >> "$STDOUT_LOG_FILE" 2>&1 &
    disown $!
    echo $! >"$PID_FILE"
else
    exec "${RUN_CMD[@]}"
fi
