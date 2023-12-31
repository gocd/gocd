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

enum Architecture {
  all('all', 'noarch', null),
  x64('x64', 'amd64', 'x86-64'),
  aarch64('aarch64', 'arm64', 'arm-64'),
  dockerDynamic('$(uname -m | sed -e s/86_//g)', '${TARGETARCH}', null)

  final String canonicalName
  final String dockerAlias
  final String tanukiWrapperAlias

  Architecture(String canonicalName, String dockerAlias, String tanukiWrapperAlias) {
    this.canonicalName = canonicalName
    this.dockerAlias = dockerAlias
    this.tanukiWrapperAlias = tanukiWrapperAlias
  }

  static current() {
    canonicalize(System.getProperty("os.arch"))
  }

  static Architecture canonicalize(String arch) {
    switch (arch.trim().toLowerCase()) {
      case ['all', 'noarch', 'none', 'N/A', '-', '']: return all
      case ['x64', 'amd64', 'x86_64', 'x86-64']: return x64
      case ['aarch64', 'arm64', 'arm64/v8', 'arm64v8', 'armv8a']: return aarch64
      default: throw IllegalArgumentException("Arch/machine type [${arch}] is unknown or not supported.")
    }
  }

  static String dockerAliasToWrapperArchAsShell() {
    '$(if ' +
      values()
        .findAll { it !in [all, dockerDynamic] }
        .collect { "[ \$TARGETARCH == ${it.dockerAlias} ]; then echo ${it.tanukiWrapperAlias};" }
        .join(" elif ") +
      ' else echo $TARGETARCH is unknown!; exit 1; fi)'
  }
}