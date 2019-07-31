#!/bin/bash

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

yell() { echo "$0: $*" >&2; }
die() { yell "$*"; exit 111; }
try() { echo "$ $@" 1>&2; "$@" || die "cannot $*"; }

declare -a _stringToArgs
function stringToArgsArray() {
  _stringToArgs=("$@")
}

setup_autoregister_properties_file_for_elastic_agent() {
  echo "agent.auto.register.key=${GO_EA_AUTO_REGISTER_KEY}" >> $1
  echo "agent.auto.register.environments=${GO_EA_AUTO_REGISTER_ENVIRONMENT}" >> $1
  echo "agent.auto.register.elasticAgent.agentId=${GO_EA_AUTO_REGISTER_ELASTIC_AGENT_ID}" >> $1
  echo "agent.auto.register.elasticAgent.pluginId=${GO_EA_AUTO_REGISTER_ELASTIC_PLUGIN_ID}" >> $1
  echo "agent.auto.register.hostname=${AGENT_AUTO_REGISTER_HOSTNAME}" >> $1

  export GO_SERVER_URL="${GO_EA_SERVER_URL}"
  # unset variables, so we don't pollute and leak sensitive stuff to the agent process...
  unset GO_EA_AUTO_REGISTER_KEY GO_EA_AUTO_REGISTER_ENVIRONMENT GO_EA_AUTO_REGISTER_ELASTIC_AGENT_ID GO_EA_AUTO_REGISTER_ELASTIC_PLUGIN_ID GO_EA_SERVER_URL AGENT_AUTO_REGISTER_HOSTNAME
}

setup_autoregister_properties_file_for_normal_agent() {
  echo "agent.auto.register.key=${AGENT_AUTO_REGISTER_KEY}" >> $1
  echo "agent.auto.register.resources=${AGENT_AUTO_REGISTER_RESOURCES}" >> $1
  echo "agent.auto.register.environments=${AGENT_AUTO_REGISTER_ENVIRONMENTS}" >> $1
  echo "agent.auto.register.hostname=${AGENT_AUTO_REGISTER_HOSTNAME}" >> $1

  # unset variables, so we don't pollute and leak sensitive stuff to the agent process...
  unset AGENT_AUTO_REGISTER_KEY AGENT_AUTO_REGISTER_RESOURCES AGENT_AUTO_REGISTER_ENVIRONMENTS AGENT_AUTO_REGISTER_HOSTNAME
}

setup_autoregister_properties_file() {
  if [ -n "$GO_EA_SERVER_URL" ]; then
    setup_autoregister_properties_file_for_elastic_agent "$1"
  else
    setup_autoregister_properties_file_for_normal_agent "$1"
  fi
}

if [ -e /run-docker-daemon.sh ]; then
  sudo /run-docker-daemon.sh
fi

AGENT_WORK_DIR="/go"

# no arguments are passed so assume user wants to run the gocd agent
# we prepend "/${AGENT_WORK_DIR}/bin/go-agent console" to the argument list
if [[ $# -eq 0 ]] ; then
  set -- "${AGENT_WORK_DIR}/bin/go-agent" console "$@"
fi

if [ "$1" = "${AGENT_WORK_DIR}/bin/go-agent" ]; then

  [ -z "${VOLUME_DIR}" ] && VOLUME_DIR="/godata"

  agent_data_dirs=(config logs pipelines)

  yell "Creating directories and symlinks to hold GoCD configuration, data, and logs"

  for each_dir in "${agent_data_dirs[@]}"; do
    if [ ! -e "${VOLUME_DIR}/${each_dir}" ]; then
      try mkdir -v "${VOLUME_DIR}/${each_dir}"
    fi

    if [ ! -e "${AGENT_WORK_DIR}/${each_dir}" ]; then
      try ln -sv "${VOLUME_DIR}/${each_dir}" "${AGENT_WORK_DIR}/${each_dir}"
    fi
  done

  wrapper_dirs=(bin lib run wrapper wrapper-config)

  yell "Creating directories and symlinks to hold GoCD wrapper binaries"

  for each_dir in "${wrapper_dirs[@]}"; do
    if [ ! -e "${AGENT_WORK_DIR}/${each_dir}" ]; then
      try ln -sv "/go-agent/${each_dir}" "${AGENT_WORK_DIR}/${each_dir}"
    fi
  done

  if [ ! -e "${AGENT_WORK_DIR}/config/agent-bootstrapper-logback-include.xml" ]; then
    try cp -rfv "/go-agent/config/agent-bootstrapper-logback-include.xml" "${AGENT_WORK_DIR}/config/agent-bootstrapper-logback-include.xml"
  fi

  if [ ! -e "${AGENT_WORK_DIR}/config/agent-launcher-logback-include.xml" ]; then
    try cp -rfv "/go-agent/config/agent-launcher-logback-include.xml" "${AGENT_WORK_DIR}/config/agent-launcher-logback-include.xml"
  fi

  if [ ! -e "${AGENT_WORK_DIR}/config/agent-logback-include.xml" ]; then
    try cp -rfv "/go-agent/config/agent-logback-include.xml" "${AGENT_WORK_DIR}/config/agent-logback-include.xml"
  fi

  setup_autoregister_properties_file "${AGENT_WORK_DIR}/config/autoregister.properties"

  yell "Running custom scripts in /docker-entrypoint.d/ ..."

  # to prevent expansion to literal string `/docker-entrypoint.d/*` when there is nothing matching the glob
  shopt -s nullglob

  for file in /docker-entrypoint.d/*; do
    if [ -f "$file" ] && [ -x "$file" ]; then
      try "$file"
    else
      yell "Ignoring $file, it is either not a file or is not executable"
    fi
  done

  # setup the java binary and wrapper log
  try sed -i \
    -e "s@wrapper.logfile=.*@/wrapper.logfile=${AGENT_WORK_DIR}/logs/go-agent-bootstrapper-wrapper.log@g" \
    -e "s@wrapper.java.command=.*@wrapper.java.command=${GO_JAVA_HOME}/bin/java@g" \
    -e "s@wrapper.working.dir=.*@wrapper.working.dir=${AGENT_WORK_DIR}@g" \
    /go-agent/wrapper-config/wrapper.conf

  echo "wrapper.app.parameter.100=-serverUrl" > /go-agent/wrapper-config/wrapper-properties.conf
  echo "wrapper.app.parameter.101=${GO_SERVER_URL}" >> /go-agent/wrapper-config/wrapper-properties.conf

  # parse/split an environment var to an array like how it should pass to the CLI
  # AGENT_BOOTSTRAPPER_JVM_ARGS is mostly for advanced users.
  eval stringToArgsArray "$AGENT_BOOTSTRAPPER_JVM_ARGS"
  AGENT_BOOTSTRAPPER_JVM_ARGS=("${_stringToArgs[@]}")

  AGENT_BOOTSTRAPPER_JVM_ARGS+=("-Dgo.console.stdout=true")
  for array_index in "${!AGENT_BOOTSTRAPPER_JVM_ARGS[@]}"
  do
    tanuki_index=$(($array_index + 100))
    echo "wrapper.java.additional.${tanuki_index}=${AGENT_BOOTSTRAPPER_JVM_ARGS[$array_index]}" >> /go-agent/wrapper-config/wrapper-properties.conf
  done

  echo "set.AGENT_STARTUP_ARGS=%AGENT_STARTUP_ARGS% -Dgo.console.stdout=true %GOCD_AGENT_JVM_OPTS%"
fi

try exec /usr/local/sbin/tini -g -- "$@"
