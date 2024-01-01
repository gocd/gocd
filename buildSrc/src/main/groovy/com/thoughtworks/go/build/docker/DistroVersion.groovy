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

package com.thoughtworks.go.build.docker

class DistroVersion implements Serializable {
  String version
  String releaseName
  Date eolDate
  boolean continueToBuild
  List<String> installPrerequisitesCommands = []

  boolean isAboutToEol() {
    return eolDate.before(new Date() + 180)
  }

  boolean isEol() {
    return eolDate.before(new Date())
  }

  boolean isPastEolGracePeriod() {
    // Allow a 30 day grace period after EOL where we can keep building
    return eolDate.before(new Date() - 30)
  }

  boolean lessThan(int target) {
    return Integer.parseInt(version) < target
  }
}
