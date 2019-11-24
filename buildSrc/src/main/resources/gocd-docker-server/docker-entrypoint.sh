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

SERVER_WORK_DIR="/go-working-dir"

# no arguments are passed so assume user wants to run the gocd server
# we prepend "${SERVER_WORK_DIR}/bin/go-server console" to the argument list
if [[ $# -eq 0 ]] ; then
  set -- "${SERVER_WORK_DIR}/bin/go-server" console "$@"
fi

if [ "$1" = "${SERVER_WORK_DIR}/bin/go-server" ]; then
  VOLUME_DIR="/godata"

  server_data_dirs=(artifacts config db logs plugins addons)

  yell "Creating directories and symlinks to hold GoCD configuration, data, and logs"

  for each_dir in "${server_data_dirs[@]}"; do
    if [ ! -e "${VOLUME_DIR}/${each_dir}" ]; then
      try mkdir -v -p "${VOLUME_DIR}/${each_dir}"
    fi

    if [ ! -e "${SERVER_WORK_DIR}/${each_dir}" ]; then
      try ln -sv "${VOLUME_DIR}/${each_dir}" "${SERVER_WORK_DIR}/${each_dir}"
    fi
  done

  wrapper_dirs=(bin lib run wrapper wrapper-config)

  yell "Creating directories and symlinks to hold GoCD wrapper binaries"

  for each_dir in "${wrapper_dirs[@]}"; do
    if [ ! -e "${SERVER_WORK_DIR}/${each_dir}" ]; then
      try ln -sv "/go-server/${each_dir}" "${SERVER_WORK_DIR}/${each_dir}"
    fi
  done

  if [ ! -e "${SERVER_WORK_DIR}/config/logback-include.xml" ]; then
    try cp -rfv "/go-server/config/logback-include.xml" "${SERVER_WORK_DIR}/config/logback-include.xml"
  fi

  try install-gocd-plugins
  try git-clone-config

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
    -e "s@wrapper.logfile=.*@/wrapper.logfile=${SERVER_WORK_DIR}/logs/go-server-wrapper.log@g" \
    -e "s@wrapper.java.command=.*@wrapper.java.command=${GO_JAVA_HOME}/bin/java@g" \
    -e "s@wrapper.working.dir=.*@wrapper.working.dir=${SERVER_WORK_DIR}@g" \
    /go-server/wrapper-config/wrapper.conf

  # parse/split an environment var to an array like how it should pass to the CLI
  # GOCD_SERVER_JVM_OPTS is mostly for advanced users.
  eval stringToArgsArray "$GOCD_SERVER_JVM_OPTS"
  GOCD_SERVER_JVM_OPTS=("${_stringToArgs[@]}")


  GOCD_SERVER_JVM_OPTS+=("-Dgo.console.stdout=true")

  # write out each system property using its own index
  for array_index in "${!GOCD_SERVER_JVM_OPTS[@]}"
  do
    tanuki_index=$(($array_index + 100))
    echo "wrapper.java.additional.${tanuki_index}=${GOCD_SERVER_JVM_OPTS[$array_index]}" >> /go-server/wrapper-config/wrapper-properties.conf
  done

fi

try exec /usr/local/sbin/tini -g -- "$@"
