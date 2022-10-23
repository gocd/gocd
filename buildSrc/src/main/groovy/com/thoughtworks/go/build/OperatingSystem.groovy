/*
 * Copyright 2021 Thoughtworks, Inc.
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
  windows("zip", "windows"),
  linux("tar.gz", "linux"),
  alpine_linux("tar.gz", "alpine-linux"),
  mac("tar.gz", "mac")

  final String extension
  final String adoptiumAlias

  OperatingSystem(String extension, String adoptiumAlias) {
    this.extension = extension
    this.adoptiumAlias = adoptiumAlias
  }
}

class OperatingSystemHelper {
  private static final org.gradle.internal.os.OperatingSystem CURRENT_OS = org.gradle.internal.os.OperatingSystem.current()

  static Map<String, Object> normalizeEnvironmentPath(Map<String, Object> environment) {
    if (CURRENT_OS.isWindows()) {
      // because windows PATH variable is case-insensitive, so we force it to be `PATH` instead of `Path` or whatever else
      def pathVariableName = environment.keySet().find { eachKey -> eachKey.toUpperCase().equals("PATH")}
      if (pathVariableName != null) {
        environment['PATH'] = environment.remove(pathVariableName)
      }
    }
    return environment
  }
}