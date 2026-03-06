#!/usr/bin/env bash
#
# Copyright Thoughtworks, Inc.
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
#
set -euo pipefail
GOCD_ROOT=$(cd `dirname $0`/.. && pwd)
TRIVY_IGNORE=.trivyignore.yaml

cp "${GOCD_ROOT}/build-platform/.trivyignore.yaml" "${TRIVY_IGNORE}"

grep -oE '<(cve|vulnerabilityName)>(CVE|GHSA).*</(cve|vulnerabilityName)>' "${GOCD_ROOT}/build-platform/dependency-check-suppress.xml" \
  | sed -E 's/<(cve|vulnerabilityName)>(.*)<\/(cve|vulnerabilityName)>/  - id: \2/' \
  >> "${TRIVY_IGNORE}"

pushd "${GOCD_ROOT}"
unset GO_SERVER_URL
./gradlew configureDockerRegistryMirror --console=colored
popd

docker volume create trivycache >/dev/null
for image in "${SCAN_ROOT}"/*.tar; do
  echo "Scanning $image..."
  rm -rf "$image-unpacked" && mkdir "$image-unpacked"
  tar xf "$image" --directory "$image-unpacked"
  docker run --rm \
      --mount type=volume,src=trivycache,dst=/root/.cache \
      --mount "type=bind,src=$(pwd)/${TRIVY_IGNORE},dst=/${TRIVY_IGNORE}" \
      --mount "type=bind,src=$(pwd)/$image-unpacked,dst=/$image-unpacked" \
    docker.io/aquasec/trivy:latest \
    image \
      --input "/${image}-unpacked" \
      --no-progress \
      --skip-version-check \
      --disable-telemetry \
      --scanners vuln,misconfig,secret \
      --ignorefile "/${TRIVY_IGNORE}" \
      --ignore-status not_affected,under_investigation,will_not_fix \
      --exit-code ${EXIT_CODE_ON_ERROR}
  echo "Scanned $image."
  echo "─────────────────────────────────────────────────────────────────"
  echo ""
done
