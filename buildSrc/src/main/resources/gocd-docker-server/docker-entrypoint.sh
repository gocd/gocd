#!/bin/bash

# Copyright 2018 ThoughtWorks, Inc.
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

VOLUME_DIR="/godata"


# no arguments are passed so assume user wants to run the gocd server
# we prepend "/go-server/server.sh" to the argument list
if [[ $# -eq 0 ]] ; then
  set -- /go-server/server.sh "$@"
fi

# if running go server as root, then initialize directory structure and call ourselves as `go` user
if [ "$1" = '/go-server/server.sh' ]; then

  if [ "$(id -u)" = '0' ]; then
    export SERVER_WORK_DIR="/go-working-dir"
    export GO_CONFIG_DIR="/go-working-dir/config"

    server_dirs=(artifacts config db logs plugins addons)

    yell "Creating directories and symlinks to hold GoCD configuration, data, and logs"

    # ensure working dir exist
    if [ ! -e "${SERVER_WORK_DIR}" ]; then
      try mkdir "${SERVER_WORK_DIR}"
      try chown go:go "${SERVER_WORK_DIR}"
    fi

    # ensure proper directory structure in the volume directory
    if [ ! -e "${VOLUME_DIR}" ]; then
      try mkdir "${VOLUME_DIR}"
      try chown go:go "${VOLUME_DIR}"
    fi

    for each_dir in "${server_dirs[@]}"; do
      if [ ! -e "${VOLUME_DIR}/${each_dir}" ]; then
        try mkdir -v "${VOLUME_DIR}/${each_dir}"
        try chown go:go "${VOLUME_DIR}/${each_dir}"
      fi

      if [ ! -e "${SERVER_WORK_DIR}/${each_dir}" ]; then
        try ln -sv "${VOLUME_DIR}/${each_dir}" "${SERVER_WORK_DIR}/${each_dir}"
        try chown go:go "${SERVER_WORK_DIR}/${each_dir}"
      fi
    done

    if [ ! -e "${SERVER_WORK_DIR}/config/logback-include.xml" ]; then
      try cp -rfv "/go-server/config/logback-include.xml" "${SERVER_WORK_DIR}/config/logback-include.xml"
      try chown go:go "${VOLUME_DIR}/config/logback-include.xml"
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

    try exec /usr/local/sbin/tini -- /usr/local/sbin/gosu go "$0" "$@"
  fi
fi

# these 3 vars are used by `/go-server/server.sh`, so we export
export GO_SERVER_SYSTEM_PROPERTIES="${GO_SERVER_SYSTEM_PROPERTIES}${GO_SERVER_SYSTEM_PROPERTIES:+ }-Dgo.console.stdout=true"

try exec "$@"
