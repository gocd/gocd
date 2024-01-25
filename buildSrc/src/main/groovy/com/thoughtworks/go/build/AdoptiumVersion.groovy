/*
 * Copyright 2024 Thoughtworks, Inc.
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

class AdoptiumVersion implements Serializable {
  int feature // e.g 17
  Integer interim // e.g null, or 0
  Integer update  // e.g null, or 4
  Integer patch   // e.g null, or 1
  int build   // e.g 8

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

  def toDownloadURLFor(OperatingSystem os, Architecture arch) {
    "https://github.com/adoptium/temurin${feature}-binaries/releases/download/" +
      "jdk-${urlSafeDisplayVersion()}/" +
      "OpenJDK${feature}${featureSuffix()}-jre_${arch.canonicalName}_${os.adoptiumAlias}_hotspot_${fileSafeDisplayVersion()}.${os.extension}"
  }

  @SuppressWarnings('unused') // Used in Gradle build scripts
  def toSha256SumURLFor(OperatingSystem os, Architecture arch) {
    "${toDownloadURLFor(os, arch)}.sha256.txt"
  }

  def toMetadata() {
    [
      included      : true,
      featureVersion: feature,
      fullVersion   : canonicalDisplayVersion(),
    ]
  }

  def toLicenseMetadata() {
    [
      "moduleName": "net.adoptium:eclipse-temurin-jre",
      "moduleVersion": canonicalDisplayVersion(),
      "moduleUrls": [
        "https://adoptium.net/",
        "https://adoptium.net/about/"
      ],
      "moduleLicenses": [
        [
          "moduleLicense": "GPLv2 with the Classpath Exception",
          "moduleLicenseUrl": "https://openjdk.org/legal/gplv2+ce.html"
        ]
      ]
    ]
  }

  static def noneMetadata() {
    [ included: false ]
  }
}

