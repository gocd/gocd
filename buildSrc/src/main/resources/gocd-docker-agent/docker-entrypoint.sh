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
    try chown go:go $1
  else
    setup_autoregister_properties_file_for_normal_agent "$1"
    try chown go:go $1
  fi
}

[ -z "${VOLUME_DIR}" ] && VOLUME_DIR="/godata"

AGENT_WORK_DIR="/go"

if [ -e /run-docker-daemon.sh ]; then
  /run-docker-daemon.sh
fi

# no arguments are passed so assume user wants to run the gocd server
# we prepend "/go-agent/agent.sh" to the argument list
if [[ $# -eq 0 ]] ; then
  set -- /go-agent/agent.sh "$@"
fi

# if running go server as root, then initialize directory structure and call ourselves as `go` user
if [ "$1" = '/go-agent/agent.sh' ]; then

  if [ "$(id -u)" = '0' ]; then
    server_dirs=(config logs pipelines)

    yell "Creating directories and symlinks to hold GoCD configuration, data, and logs"

    # ensure working dir exist
    if [ ! -e "${AGENT_WORK_DIR}" ]; then
      try mkdir "${AGENT_WORK_DIR}"
      try chown go:go "${AGENT_WORK_DIR}"
    fi

    # ensure proper directory structure in the volume directory
    if [ ! -e "${VOLUME_DIR}" ]; then
      try mkdir "${VOLUME_DIR}"
      try chown go:go "${AGENT_WORK_DIR}"
    fi

    for each_dir in "${server_dirs[@]}"; do
      if [ ! -e "${VOLUME_DIR}/${each_dir}" ]; then
        try mkdir -v "${VOLUME_DIR}/${each_dir}"
        try chown go:go "${VOLUME_DIR}/${each_dir}"
      fi

      if [ ! -e "${AGENT_WORK_DIR}/${each_dir}" ]; then
        try ln -sv "${VOLUME_DIR}/${each_dir}" "${AGENT_WORK_DIR}/${each_dir}"
        try chown go:go "${AGENT_WORK_DIR}/${each_dir}"
      fi
    done

    if [ ! -e "${AGENT_WORK_DIR}/config/agent-bootstrapper-logback-include.xml" ]; then
      try cp -rfv "/go-agent/config/agent-bootstrapper-logback-include.xml" "${AGENT_WORK_DIR}/config/agent-bootstrapper-logback-include.xml"
      try chown go:go "${VOLUME_DIR}/config/agent-bootstrapper-logback-include.xml"
    fi

    if [ ! -e "${AGENT_WORK_DIR}/config/agent-launcher-logback-include.xml" ]; then
      try cp -rfv "/go-agent/config/agent-launcher-logback-include.xml" "${AGENT_WORK_DIR}/config/agent-launcher-logback-include.xml"
      try chown go:go "${VOLUME_DIR}/config/agent-launcher-logback-include.xml"
    fi

    if [ ! -e "${AGENT_WORK_DIR}/config/agent-logback-include.xml" ]; then
      try cp -rfv "/go-agent/config/agent-logback-include.xml" "${AGENT_WORK_DIR}/config/agent-logback-include.xml"
      try chown go:go "${VOLUME_DIR}/config/agent-logback-include.xml"
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

    try exec /usr/local/sbin/tini -- /usr/local/sbin/gosu go "$0" "$@"
  fi
fi

# these 3 vars are used by `/go-agent/agent.sh`, so we export
export AGENT_WORK_DIR
export GO_AGENT_SYSTEM_PROPERTIES="${GO_AGENT_SYSTEM_PROPERTIES}${GO_AGENT_SYSTEM_PROPERTIES:+ }-Dgo.console.stdout=true"
export AGENT_BOOTSTRAPPER_JVM_ARGS="${AGENT_BOOTSTRAPPER_JVM_ARGS}${AGENT_BOOTSTRAPPER_JVM_ARGS:+ }-Dgo.console.stdout=true"

try exec "$@"
