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
  List<String> getLinuxJvmArgs() {
    [
      '-Dgocd.server.log.dir=/var/log/go-server',
      '-Dcruise.config.dir=/etc/go',
      '-Dcruise.config.file=/etc/go/cruise-config.xml',
    ]
  }

  @Override
  boolean getAllowPassthrough() {
    false
  }

  @Override
  Map<String, Permission> getDirectories() {
    [
      '/usr/share/doc/go-server'           : perm(mode: 0755, owner: 'root', group: 'root'),
      '/usr/share/go-server/wrapper-config': perm(mode: 0750, owner: 'root', group: 'go'),
      '/var/lib/go-server'                 : perm(mode: 0750, owner: 'go',   group: 'go'),
      '/var/lib/go-server/run'             : perm(mode: 0750, owner: 'go',   group: 'go'),
      '/var/log/go-server'                 : perm(mode: 0750, owner: 'go',   group: 'go'),
      '/var/run/go-server'                 : perm(mode: 0750, owner: 'go',   group: 'go'),
      '/etc/go'                            : perm(mode: 0750, owner: 'go',   group: 'go'),
      '/var/go'                            : perm(mode: 0750, owner: 'go',   group: 'go'),
    ]
  }

  @Override
  Map<String, Permission> getConfigFiles() {
    [
      '/usr/share/go-server/wrapper-config/wrapper.conf'           : perm(mode: 0640, owner: 'root', group: 'go'),
      '/usr/share/go-server/wrapper-config/wrapper-properties.conf': perm(mode: 0640, owner: 'root', group: 'go'),
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
