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


MANUAL_SETTING=${MANUAL_SETTING:-"N"}

if [ "$MANUAL_SETTING" == "N" ]; then
    if [ -f /etc/default/go-server ]; then
        echo "using default settings from /etc/default/go-server"
        . /etc/default/go-server
    fi
fi

CWD=`dirname "$0"`
SERVER_DIR=`(cd "$CWD" && pwd)`

[ ! -z $SERVER_MEM ] || SERVER_MEM="512m"
[ ! -z $SERVER_MAX_MEM ] || SERVER_MAX_MEM="1024m"
[ ! -z $SERVER_MAX_PERM_GEN ] || SERVER_MAX_PERM_GEN="256m"
[ ! -z $SERVER_MIN_PERM_GEN ] || SERVER_MIN_PERM_GEN="128m"
[ ! -z $GO_SERVER_PORT ] || GO_SERVER_PORT="8153"
[ ! -z $GO_SERVER_SSL_PORT ] || GO_SERVER_SSL_PORT="8154"
[ ! -z "$SERVER_WORK_DIR" ] || SERVER_WORK_DIR="$SERVER_DIR"
[ ! -z "$YOURKIT_DISBALE_TRACING" ] || YOURKIT_DISBALE_TRACING=""

if [ -d /var/log/go-server ]; then
    LOG_FILE=/var/log/go-server/go-server.log
else
    LOG_FILE=go-server.log
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
    JVM_DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
else
    JVM_DEBUG=""
fi

if [ "$GC_LOG" != "" ]; then
    GC_LOG="-verbose:gc -Xloggc:go-server-gc.log -XX:+PrintGCTimeStamps -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGC"
else
    GC_LOG=""
fi

if [ ! -z $SERVER_LISTEN_HOST ]; then
    GO_SERVER_SYSTEM_PROPERTIES="$GO_SERVER_SYSTEM_PROPERTIES -Dcruise.listen.host=$SERVER_LISTEN_HOST"
fi
SERVER_STARTUP_ARGS+=("-server $YOURKIT")
SERVER_STARTUP_ARGS+=("-Xms$SERVER_MEM -Xmx$SERVER_MAX_MEM -XX:PermSize=$SERVER_MIN_PERM_GEN -XX:MaxPermSize=$SERVER_MAX_PERM_GEN")
SERVER_STARTUP_ARGS+=("$JVM_DEBUG $GC_LOG $GO_SERVER_SYSTEM_PROPERTIES")
SERVER_STARTUP_ARGS+=("-Duser.language=en -Dorg.mortbay.jetty.Request.maxFormContentSize=30000000 -Djruby.rack.request.size.threshold.bytes=30000000 -Djruby.compile.mode=FORCE")
SERVER_STARTUP_ARGS+=("-Duser.country=US -Dcruise.config.dir=$GO_CONFIG_DIR -Dcruise.config.file=$GO_CONFIG_DIR/cruise-config.xml")
SERVER_STARTUP_ARGS+=("-Dcruise.server.port=$GO_SERVER_PORT -Dcruise.server.ssl.port=$GO_SERVER_SSL_PORT")
if [ "$TMPDIR" != "" ]; then
    SERVER_STARTUP_ARGS+=("-Djava.io.tmpdir=$TMPDIR")
fi
CMD="$JAVA_HOME/bin/java ${SERVER_STARTUP_ARGS[@]} -jar $SERVER_DIR/go.jar"

echo "Starting Go Server with command: $CMD" >>$LOG_FILE
echo "Starting Go Server in directory: $GO_WORK_DIR" >>$LOG_FILE
cd "$SERVER_WORK_DIR"

if [ "$JAVA_HOME" == "" ]; then
    echo "Please set JAVA_HOME to proceed."
    exit 1
fi

if [ "$DAEMON" == "Y" ]; then
    eval "nohup $CMD >>$LOG_FILE &"
    echo $! >$PID_FILE
else
    eval "$CMD"
fi

