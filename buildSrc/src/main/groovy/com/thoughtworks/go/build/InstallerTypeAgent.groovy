/*
 * Copyright 2021 ThoughtWorks, Inc.
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

class InstallerTypeAgent implements InstallerType {

  @Override
  String getBaseName() {
    'go-agent'
  }

  @Override
  String getJarFileName() {
    'agent-bootstrapper.jar'
  }

  @Override
  String getLogFileName() {
    'go-agent-bootstrapper-wrapper.log'
  }

  @Override
  Map<String, String> getAdditionalEnvVars() {
    [
      AGENT_STARTUP_ARGS: '-Xms128m -Xmx256m'
    ]
  }

  @Override
  Map<String, String> getAdditionalLinuxEnvVars() {
    [
      AGENT_STARTUP_ARGS: '-Xms128m -Xmx256m -Dgocd.agent.log.dir=/var/log/go-agent'
    ]
  }

  @Override
  List<String> getJvmArgs() {
    []
  }

  @Override
  List<String> getLinuxJvmArgs() {
    [
      '-Dgocd.agent.log.dir=/var/log/go-agent'
    ]
  }

  @Override
  boolean getAllowPassthrough() {
    return true
  }

  @Override
  Map<String, Object> getDirectories() {
    [
      '/usr/share/doc/go-agent'           : [mode: 0755, owner: 'root', group: 'root', ownedByPackage: true],
      '/usr/share/go-agent/wrapper-config': [mode: 0750, owner: 'root', group: 'go', ownedByPackage: true],
      '/var/lib/go-agent'                 : [mode: 0750, owner: 'go', group: 'go', ownedByPackage: true],
      '/var/lib/go-agent/run'             : [mode: 0750, owner: 'go', group: 'go', ownedByPackage: true],
      '/var/log/go-agent'                 : [mode: 0750, owner: 'go', group: 'go', ownedByPackage: true],
      '/var/run/go-agent'                 : [mode: 0750, owner: 'go', group: 'go', ownedByPackage: true]
    ]
  }

  @Override
  Map<String, Object> getConfigFiles() {
    [
      '/usr/share/go-agent/wrapper-config/wrapper.conf'           : [mode: 0640, owner: 'root', group: 'go', ownedByPackage: true, confFile: true],
      '/usr/share/go-agent/wrapper-config/wrapper-properties.conf': [mode: 0640, owner: 'root', group: 'go', ownedByPackage: true, confFile: true],
    ]
  }

  @Override
  String getPackageDescription() {
    '''
    GoCD Agent Component
    Next generation continuous integration and release management server from ThoughtWorks.
    '''.stripIndent().trim()
  }

  @Override
  String getWindowsAndOSXServiceName() {
    return 'Go Agent'
  }
}
