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

class InstallerTypeServer implements InstallerType {
  static instance = new InstallerTypeServer()

  @Override
  String getName() {
    'server'
  }

  @Override
  String getBaseName() {
    'go-server'
  }

  @Override
  String getJarFileName() {
    'go.jar'
  }

  @Override
  String getLogFileName() {
    'go-server-wrapper.log'
  }

  @Override
  Map<String, String> getAdditionalEnvVars() {
    [:]
  }

  @Override
  Map<String, String> getAdditionalLinuxEnvVars() {
    [:]
  }

  @Override
  List<String> getJvmInternalAccessArgs() {
    [
      '--add-opens=java.base/java.lang=ALL-UNNAMED', // Required for Hibernate 3.6/Javassist proxying, ConsoleResult exception smudging, GoConfigGraphWalker (at minimum, may be used for other things)
      '--add-opens=java.base/java.util=ALL-UNNAMED', // Required at least for cloning GoConfig subclasses of java.util classes :(
      '--enable-native-access=ALL-UNNAMED',          // JDK 25+: Needed by com.kenai.jffi.internal.StubLoader at least
      '--sun-misc-unsafe-memory-access=allow',       // JDK 25+: sun.misc.Unsafe needed by Felix SecureAction, object cloning and probably others
      '-XX:+IgnoreUnrecognizedVMOptions',            // JDK <25: Allow use of --sun-misc-unsafe-memory-access on older JVMs without errors
    ]
  }

  @Override
  List<String> getJvmArgs() {
    getJvmInternalAccessArgs() + [
      '-Xms512m',
      '-Xmx1024m',
      '-XX:MaxMetaspaceSize=400m',
      '-Duser.language=en',
      '-Duser.country=US',
    ]
  }

  @Override
  InstallerLayout getLinuxPackageLayout() {
    InstallerLayout.linuxPackageLayout(baseName).tap { configDir = '/etc/go' }
  }

  @Override
  List<String> getLinuxJvmArgs() {
    def layout = linuxPackageLayout
    [
      "-Dgocd.server.log.dir=${layout.logDir}",
      "-Dcruise.config.dir=${layout.configDir}",
      "-Dcruise.config.file=${layout.configDir}/cruise-config.xml",
    ]
  }

  @Override
  boolean getAllowPassthrough() {
    false
  }

  @Override
  Map<String, Permission> getLinuxDirectoryPerms() {
    [
      (linuxPackageLayout.docsDir)                       : perm(mode: 0755, owner: 'root', group: 'root'),
      (linuxPackageLayout.installDir + '/wrapper-config'): perm(mode: 0750, owner: 'root', group: 'go'),
      (linuxPackageLayout.workingDir)                    : perm(mode: 0750, owner: 'go',   group: 'go'),
      (linuxPackageLayout.workingDir + '/run')           : perm(mode: 0750, owner: 'go',   group: 'go'),
      (linuxPackageLayout.logDir)                        : perm(mode: 0750, owner: 'go',   group: 'go'),
      (linuxPackageLayout.configDir)                     : perm(mode: 0750, owner: 'go',   group: 'go'),
      (linuxPackageLayout.dataDir)                       : perm(mode: 0750, owner: 'go',   group: 'go'),
    ]
  }

  @Override
  Map<String, Permission> getLinuxConfigFilePerms() {
    [
      (linuxPackageLayout.installDir + '/wrapper-config/wrapper.conf')           : perm(mode: 0640, owner: 'root', group: 'go'),
      (linuxPackageLayout.installDir + '/wrapper-config/wrapper-properties.conf'): perm(mode: 0640, owner: 'root', group: 'go'),
    ]
  }

  @Override
  String getPackageDescription() {
    '''
    GoCD Server
    Component
    Next generation
    continuous integration and release management server from Thoughtworks.
    '''.stripIndent().trim()
  }

  @Override
  String getWindowsAndOSXServiceName() {
    return 'Go Server'
  }

}
