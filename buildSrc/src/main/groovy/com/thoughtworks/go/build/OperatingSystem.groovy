/*
 * Copyright Thoughtworks, Inc.
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
  windows("zip", "windows", "windows"),
  linux("tar.gz", "linux", "linux"),
  linux_alpine("tar.gz", "alpine-linux", "alpine"),
  mac("tar.gz", "mac", "macosx")

  final String extension
  final String adoptiumAlias
  final String tanukiWrapperAlias

  OperatingSystem(String extension, String adoptiumAlias, String tanukiWrapperAlias) {
    this.extension = extension
    this.adoptiumAlias = adoptiumAlias
    this.tanukiWrapperAlias = tanukiWrapperAlias
  }
}

class OperatingSystemHelper {
  static final org.gradle.internal.os.OperatingSystem CURRENT_OS = org.gradle.internal.os.OperatingSystem.current()

  static Map<String, ?> normalizeEnvironmentPath(Map<String, ?> environment) {
    if (CURRENT_OS.isWindows()) {
      // because windows PATH variable is case-insensitive, so we force it to be `PATH` instead of `Path` or whatever else
      def pathVariableName = environment.keySet().find { eachKey -> (eachKey.toUpperCase() == "PATH") }
      if (pathVariableName != null) {
        environment['PATH'] = environment.remove(pathVariableName)
      }
    }
    return environment
  }
}