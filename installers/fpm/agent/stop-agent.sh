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


if [ -f /etc/default/go-agent ]; then
    . /etc/default/go-agent
fi

CWD=`dirname "$0"`
AGENT_DIR=`(cd "$CWD" && pwd)`

AGENT_MEM=${AGENT_MEM:-"128m"}
AGENT_MAX_MEM=${AGENT_MAX_MEM:-"256m"}
GO_SERVER=${GO_SERVER:-"127.0.0.1"}
GO_SERVER_PORT=${GO_SERVER_PORT:-"8153"}
JAVA_HOME=${JAVA_HOME:-"/usr"}
ANT_HOME=${ANT_HOME:-"$AGENT_DIR/apache-ant-1.7.1"}
AGENT_WORK_DIR=${AGENT_WORK_DIR:-"$AGENT_DIR"}
VNC=${VNC:-"N"}

if [ -d /var/log/go-agent ]; then
    LOG_FILE=/var/log/go-agent/agent-bootstrapper.log
else
    LOG_FILE=$AGENT_WORK_DIR/agent-bootstrapper.log
fi

if [ "$PID_FILE" ]; then
    echo "Overriding PID_FILE with $PID_FILE"
elif [ -d /var/run/go-agent ]; then
    PID_FILE=/var/run/go-agent/go-agent.pid
else
    PID_FILE="$AGENT_WORK_DIR/go-agent.pid"
fi


cat $PID_FILE | xargs kill
