/*
 * Copyright 2022 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.build

import java.nio.charset.StandardCharsets

class AdoptiumVersion {
  Integer feature // e.g 17
  Integer interim // e.g null, or 0
  Integer update  // e.g null, or 4
  Integer patch   // e.g null, or 1
  Integer build   // e.g 8

  // Examples
  // 17+35 (first release)
  // 17.0.4+8 (normal release)
  // 17.0.4.1+1 (rare emergency patch release)
  def canonicalDisplayVersion() {
    "${[feature, interim, update, patch].findAll({ it != null }).join('.')}+${build}"
  }

  // Examples
  // 17%2B35 (first release)
  // 17.0.4%2B8 (normal release)
  // 17.0.4.1%2B1 (rare emergency patch release)
  def urlSafeDisplayVersion() {
    URLEncoder.encode(canonicalDisplayVersion(), StandardCharsets.UTF_8)
  }

  // Examples
  // 17_35 (first release)
  // 17.0.4_8 (normal release)
  // 17.0.4.1_1 (rare emergency patch release
  def fileSafeDisplayVersion() {
    canonicalDisplayVersion().replace('+', '_')
  }

  def featureSuffix() {
    update == null ? '' : 'U'
  }

  def toDownloadURLFor(OperatingSystem os, AdoptiumArch arch = AdoptiumArch.x64) {
    "https://github.com/adoptium/temurin${feature}-binaries/releases/download/" +
      "jdk-${urlSafeDisplayVersion()}/" +
      "OpenJDK${feature}${featureSuffix()}-jre_${arch.adoptiumAlias}_${os.adoptiumAlias()}_hotspot_${fileSafeDisplayVersion()}.${os.extension}"
  }

  def toSha256SumURLFor(OperatingSystem os, AdoptiumArch arch = AdoptiumArch.x64) {
    "${toDownloadURLFor(os, arch)}.sha256.txt"
  }
}

enum AdoptiumArch {
  x64('x64'),
  aarch64('aarch64')

  String adoptiumAlias

  AdoptiumArch(String adoptiumAlias) {
    this.adoptiumAlias = adoptiumAlias
  }
}
