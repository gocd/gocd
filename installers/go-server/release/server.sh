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

MANUAL_SETTING=${MANUAL_SETTING:-"N"}

if [ "$MANUAL_SETTING" == "N" ]; then
    if [ -f /etc/default/go-server ]; then
        echo "[$(date)] using default settings from /etc/default/go-server"
        . /etc/default/go-server
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

CWD="$(dirname "$0")"
SERVER_DIR="$(cd "$CWD" && pwd)"

[ ! -z $SERVER_MEM ] || SERVER_MEM="512m"
[ ! -z $SERVER_MAX_MEM ] || SERVER_MAX_MEM="1024m"
[ ! -z $SERVER_MAX_PERM_GEN ] || SERVER_MAX_PERM_GEN="256m"
[ ! -z $SERVER_MIN_PERM_GEN ] || SERVER_MIN_PERM_GEN="128m"
[ ! -z $GO_SERVER_PORT ] || GO_SERVER_PORT="8153"
[ ! -z $GO_SERVER_SSL_PORT ] || GO_SERVER_SSL_PORT="8154"
[ ! -z "$SERVER_WORK_DIR" ] || SERVER_WORK_DIR="$SERVER_DIR"
[ ! -z "$YOURKIT_DISABLE_TRACING" ] || YOURKIT_DISABLE_TRACING=""

if [ -d /var/log/go-server ]; then
    STDOUT_LOG_FILE=/var/log/go-server/go-server.out.log
else
    STDOUT_LOG_FILE=go-server.out.log
fi

if [ "$PID_FILE" ]; then
    echo "Overriding PID_FILE with $PID_FILE"
elif [ -d /var/run/go-server ]; then
    PID_FILE=/var/run/go-server/go-server.pid
else
    PID_FILE=go-server.pid
fi

if [ -d /etc/go ]; then
    GO_CONFIG_DIR=/etc/go
else
    GO_CONFIG_DIR=$SERVER_DIR/config
fi

if [ -e /usr/lib/yourkit/libyjpagent.jnilib ]; then
    YOURKIT_PATH="/usr/lib/yourkit/libyjpagent.jnilib"
elif [ -e /usr/lib/yourkit/libyjpagent.so ]; then
    YOURKIT_PATH="/usr/lib/yourkit/libyjpagent.so"
fi

YJP_PARAMS_9="
YOURKIT_DO_NOT_disabletracing
YOURKIT_DO_NOT_disablealloc
YOURKIT_DO_NOT_disablej2ee
YOURKIT_DO_NOT_disableexceptiontelemetry
"

YJP_PARAMS_8="
YOURKIT_DO_NOT_disablecounts
YOURKIT_DO_NOT_disablealloc
YOURKIT_DO_NOT_disablej2ee
YOURKIT_DO_NOT_disableexceptiontelemetry
"

if [ "$YOURKIT_PATH" != "" ]; then
    YOURKIT="-agentpath:$YOURKIT_PATH=port=6133,builtinprobes=none"
    [ ! -z $YOURKIT_VERSION ] || YOURKIT_VERSION="9"
    if [[ $ENABLE_YOURKIT_OPTIMIZATIONS = yes ]]; then
        echo "Attempting yourkit optimizations, assuming yourkit major version $YOURKIT_VERSION"
        YJP_PARAMS_VARIABLE="YJP_PARAMS_$YOURKIT_VERSION"
        YJP_PARAMS=${!YJP_PARAMS_VARIABLE}
        for yjp_param in `echo $YJP_PARAMS`; do
            param_set=${!yjp_param}
            if [ "x$param_set" = "x" ]; then
                actual_param_name=`echo $yjp_param | sed -e 's/YOURKIT_DO_NOT_//g'`
                echo "initializing yourkit with: $actual_param_name"
                YOURKIT="$YOURKIT,$actual_param_name"
            fi
        done
    fi
else
    YOURKIT=""
fi

if [ "$JVM_DEBUG" != "" ]; then
    JVM_DEBUG=("-Xdebug" "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005")
else
    JVM_DEBUG=()
fi

if [ "$GC_LOG" != "" ]; then
    GC_LOG=("-verbose:gc" "-Xloggc:go-server-gc.log" "-XX:+PrintGCTimeStamps" "-XX:+PrintTenuringDistribution" "-XX:+PrintGCDetails" "-XX:+PrintGC")
else
    GC_LOG=()
fi

eval stringToArgsArray "$GO_SERVER_SYSTEM_PROPERTIES"
GO_SERVER_SYSTEM_PROPERTIES=("${_stringToArgs[@]}")

if [ ! -z $SERVER_LISTEN_HOST ]; then
    GO_SERVER_SYSTEM_PROPERTIES+=(-Dcruise.listen.host=$SERVER_LISTEN_HOST)
fi
SERVER_STARTUP_ARGS=("-server")

if [ ! -z $YOURKIT ]; then
    SERVER_STARTUP_ARGS+=("$YOURKIT")
fi
if [ "$TMPDIR" != "" ]; then
    SERVER_STARTUP_ARGS+=("-Djava.io.tmpdir=$TMPDIR")
fi
if [ "$USE_URANDOM" != "false" ] && [ -e "/dev/urandom" ]; then
    SERVER_STARTUP_ARGS+=("-Djava.security.egd=file:/dev/./urandom")
fi

SERVER_STARTUP_ARGS+=("-Xms$SERVER_MEM" "-Xmx$SERVER_MAX_MEM" "-XX:PermSize=$SERVER_MIN_PERM_GEN" "-XX:MaxPermSize=$SERVER_MAX_PERM_GEN")
SERVER_STARTUP_ARGS+=("${JVM_DEBUG[@]}" "${GC_LOG[@]}" "${GO_SERVER_SYSTEM_PROPERTIES[@]}")
SERVER_STARTUP_ARGS+=("-Duser.language=en" "-Djruby.rack.request.size.threshold.bytes=30000000")
SERVER_STARTUP_ARGS+=("-Duser.country=US" "-Dcruise.config.dir=$GO_CONFIG_DIR" "-Dcruise.config.file=$GO_CONFIG_DIR/cruise-config.xml")
SERVER_STARTUP_ARGS+=("-Dcruise.server.port=$GO_SERVER_PORT" "-Dcruise.server.ssl.port=$GO_SERVER_SSL_PORT")

RUN_CMD=("$(autoDetectJavaExecutable)" "${SERVER_STARTUP_ARGS[@]}" "-jar" "$SERVER_DIR/go.jar")

echo "[$(date)] Starting Go Server with command: ${RUN_CMD[@]}" >>"$STDOUT_LOG_FILE"
echo "[$(date)] Starting Go Server in directory: $SERVER_WORK_DIR" >> $STDOUT_LOG_FILE
cd "$SERVER_WORK_DIR"

if [ "$DAEMON" == "Y" ]; then
    exec nohup "${RUN_CMD[@]}" >> "$STDOUT_LOG_FILE" 2>&1 &
    disown $!
    echo $! >"$PID_FILE"
else
    exec "${RUN_CMD[@]}"
fi
