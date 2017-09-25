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

if [ "$2" == "service_mode" ]; then
  if [ -f /etc/default/${SERVICE_NAME} ]; then
    . /etc/default/${SERVICE_NAME}
  fi
fi

CWD=`dirname "$0"`
AGENT_DIR=`(cd "$CWD" && pwd)`

AGENT_WORK_DIR=${AGENT_WORK_DIR:-"$AGENT_DIR"}

if [ "$1" == "service_mode" ] && [ -d "/var/run/go-agent" ]; then
  PID_FILE="/var/run/go-agent/${SERVICE_NAME}.pid"
else
  PID_FILE="$AGENT_WORK_DIR/go-agent.pid"
fi

cat $PID_FILE | xargs kill
