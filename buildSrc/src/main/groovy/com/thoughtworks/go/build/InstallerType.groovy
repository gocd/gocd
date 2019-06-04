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

enum InstallerType {

  agent("Go Agent", "go-agent", "agent-bootstrapper.jar", "go-agent-bootstrapper-wrapper.log",
    [
      AGENT_STARTUP_ARGS: '-Xms128m -Xmx256m'
    ],
    [], true),

  sever("Go Server", "go-server", "go.jar", "go-server-wrapper.log",
    [:],
    [
      '-Xms512m',
      '-Xmx1024m',
      '-XX:MaxMetaspaceSize=400m',
      '-Duser.language=en',
      '-Duser.country=US',
    ], false)

  final String appName
  final String baseName
  final String jarFileName
  final String logFileName
  final Map<String, String> additionalEnvVars
  final List<String> jvmArgs
  final boolean allowPassthrough

  InstallerType(String appName,
                String baseName,
                String jarFileName,
                String logFileName,
                Map<String, String> additionalEnvVars,
                List<String> jvmArgs,
                boolean allowPassthrough) {
    this.jvmArgs = jvmArgs
    this.additionalEnvVars = additionalEnvVars
    this.logFileName = logFileName
    this.jarFileName = jarFileName
    this.baseName = baseName
    this.appName = appName
    this.allowPassthrough = allowPassthrough
  }
}
