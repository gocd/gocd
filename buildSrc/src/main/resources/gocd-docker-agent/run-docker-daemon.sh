#!/bin/bash

# Copyright 2024 Thoughtworks, Inc.
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
# shellcheck disable=SC2086
$(which dind) dockerd --host=unix:///var/run/docker.sock ${DOCKERD_ADDITIONAL_ARGS:-'--host=tcp://localhost:2375'} > /var/log/dockerd.log 2>&1 &

waited=0
until [ $waited -ge ${DOCKERD_MAX_WAIT_SECS:-30} ] || docker stats --no-stream; do
  sleep 1
  ((waited++))
done
# shellcheck disable=SC2181
if ! docker stats --no-stream; then
  echo "dockerd startup failed..."
  cat /var/log/dockerd.log
  exit 1
fi
echo "dockerd started"
disown
