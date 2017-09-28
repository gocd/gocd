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

# by default we do not daemonize
DAEMON="${DAEMON:-N}"

if [ "$1" == 'service_mode' ]; then
  if [ -f /etc/default/go-server ]; then
    . /etc/default/go-server
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

CWD="$(dirname "$0")"
SERVER_DIR="$(cd "$CWD" && pwd)"

SERVER_MEM="${SERVER_MEM:-512m}"
SERVER_MAX_MEM="${SERVER_MAX_MEM:-1g}"
SERVER_MAX_PERM_GEN="${SERVER_MAX_PERM_GEN:-256m}"
GO_SERVER_PORT="${GO_SERVER_PORT:-8153}"
GO_SERVER_SSL_PORT="${GO_SERVER_SSL_PORT:-8154}"
SERVER_WORK_DIR="${SERVER_WORK_DIR:-$SERVER_DIR}"

if [ ! -d "${SERVER_WORK_DIR}" ]; then
  echo Server working directory ${SERVER_WORK_DIR} does not exist
  exit 2
fi

if [ "$1" == 'service_mode' ] && [ -d "/var/log/go-server" ]; then
  GO_SERVER_LOG_DIR="/var/log/go-server"
else
  GO_SERVER_LOG_DIR="${SERVER_WORK_DIR}/logs"
  mkdir -p "${GO_SERVER_LOG_DIR}"
fi

export GO_SERVER_LOG_DIR

STDOUT_LOG_FILE="${GO_SERVER_LOG_DIR}/go-server.out.log"

if [ "$1" == "service_mode" ] && [ -d "/var/run/go-server" ]; then
  PID_FILE="/var/run/go-server/go-server.pid"
else
  PID_FILE="${SERVER_WORK_DIR}/go-server.pid"
fi

if [ -z "${GO_CONFIG_DIR}" ]; then
  if [ -d /etc/go ]; then
    GO_CONFIG_DIR=/etc/go
  else
    GO_CONFIG_DIR=$SERVER_DIR/config
  fi
fi

eval stringToArgsArray "$GO_SERVER_SYSTEM_PROPERTIES"
GO_SERVER_SYSTEM_PROPERTIES=("${_stringToArgs[@]}")

if [ ! -z $SERVER_LISTEN_HOST ]; then
    GO_SERVER_SYSTEM_PROPERTIES+=(-Dcruise.listen.host=$SERVER_LISTEN_HOST)
fi
SERVER_STARTUP_ARGS=("-server")

if [ "$DAEMON" == "Y" ]; then
  SERVER_STARTUP_ARGS+=("-Dgocd.redirect.stdout.to.file=$STDOUT_LOG_FILE")
fi

if [ "$TMPDIR" != "" ]; then
    SERVER_STARTUP_ARGS+=("-Djava.io.tmpdir=$TMPDIR")
fi
if [ "$USE_URANDOM" != "false" ] && [ -e "/dev/urandom" ]; then
    SERVER_STARTUP_ARGS+=("-Djava.security.egd=file:/dev/./urandom")
fi

SERVER_STARTUP_ARGS+=("-Xms$SERVER_MEM" "-Xmx$SERVER_MAX_MEM" "-XX:MaxMetaspaceSize=$SERVER_MAX_PERM_GEN")
SERVER_STARTUP_ARGS+=("${GO_SERVER_SYSTEM_PROPERTIES[@]}")
SERVER_STARTUP_ARGS+=("-Duser.language=en" "-Djruby.rack.request.size.threshold.bytes=30000000")
SERVER_STARTUP_ARGS+=("-Duser.country=US" "-Dcruise.config.dir=$GO_CONFIG_DIR" "-Dcruise.config.file=$GO_CONFIG_DIR/cruise-config.xml")
SERVER_STARTUP_ARGS+=("-Dcruise.server.port=$GO_SERVER_PORT" "-Dcruise.server.ssl.port=$GO_SERVER_SSL_PORT")

RUN_CMD=("$(autoDetectJavaExecutable)" "${SERVER_STARTUP_ARGS[@]}" "-jar" "$SERVER_DIR/go.jar")

cd "$SERVER_WORK_DIR"

if [ "$DAEMON" == "Y" ]; then
    exec nohup "${RUN_CMD[@]}" &
    disown $!
    echo $! >"$PID_FILE"
else
    exec "${RUN_CMD[@]}"
fi
