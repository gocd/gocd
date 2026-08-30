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

import groovy.transform.MapConstructor

@MapConstructor
class InstallerLayout {
  String installDir

    /**
   * Absolute, or relative to bin/ dir
   */
  String workingDir

  /**
   * Absolute, or relative to workingDir
   */
  String logDir

  /**
   * GoCD server configuration location (cruise.config.dir). Linux packages only; the generic
   * zip leaves this unset and the application defaults apply (config/ under the working dir).
   */
  String configDir

  /**
   * Shared GoCD data location (the 'go' service user's home). Linux packages only.
   */
  String dataDir

  /**
   * Absolute, or relative to workingDir
   */
  String docsDir

  String getInstallDirBinRelative() { isAbsolute() ? installDir : '..'}

  /**
   * Resolve a path within the install dir. Relative layouts must produce unprefixed paths
   * ('lib/go.jar', never './lib/go.jar'): the Tanuki Standard Edition license key validates
   * the exact values of properties such as wrapper.app.parameter.1, and it was generated
   * against the unprefixed form.
   */
  String installPathWorkingRelative(String path) { absolute ? "${installDir}/${path}" : path }

  private boolean isAbsolute() { installDir.startsWith('/') }

  static genericZipLayout() {
    new InstallerLayout(
      installDir: '.',
      workingDir: '..',
      logDir: 'logs'
    )
  }

  static linuxPackageLayout(String baseName) {
    new InstallerLayout(
      installDir: "/usr/share/${baseName}",
      workingDir: "/var/lib/${baseName}",
      logDir: "/var/log/${baseName}",
      dataDir: '/var/go',
      docsDir: "/usr/share/doc/${baseName}"
    )
  }
}
