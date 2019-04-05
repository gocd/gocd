/*
 * Copyright 2019 ThoughtWorks, Inc.
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

enum OperatingSystem {
  windows("zip"), linux("tar.gz"), mac("tar.gz")

  final String extension

  OperatingSystem(String extension) {
    this.extension = extension
  }
}

class AdoptOpenJDKUrlHelper {
  static String downloadURL(OperatingSystem operatingSystem, int majorVersion, int patchVersion) {
    "https://github.com/AdoptOpenJDK/openjdk${majorVersion}-binaries/releases/download/jdk-${majorVersion}%2B${patchVersion}/OpenJDK${majorVersion}U-jre_x64_${operatingSystem.name()}_hotspot_${majorVersion}_${patchVersion}.${operatingSystem.extension}"
  }


  static String sha256sumURL(OperatingSystem operatingSystem, int majorVersion, int patchVersion) {
    "${downloadURL(operatingSystem, majorVersion, patchVersion)}.sha256.txt"
  }
}
