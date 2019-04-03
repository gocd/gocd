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

package com.thoughtworks.go.build.docker

import com.thoughtworks.go.build.AdoptOpenJDKUrlHelper
import com.thoughtworks.go.build.OperatingSystem
import org.gradle.api.Project

trait DistroBehavior {

  List<DistroVersion> getSupportedVersions() {
    return []
  }

  DistroVersion getVersion(String version) {
    return supportedVersions.find { supportedVersion ->
      supportedVersion.version == version
    }
  }

  List<String> getCreateUserAndGroupCommands() {
    return [
      'groupadd -g ${GID} go',
      'useradd -u ${UID} -g go -d /home/go -m go'
    ]
  }

  List<String> getInstallPrerequisitesCommands() {
    throw new RuntimeException("Subclasses must implement!")
  }

  List<String> getInstallJavaCommands(Project project) {
    return [
      "curl --fail --location --silent --show-error '${AdoptOpenJDKUrlHelper.downloadURL(OperatingSystem.linux, project.versions.adoptOpenjdk.major, project.versions.adoptOpenjdk.build)}' --output /tmp/jre.tar.gz",
      'mkdir -p /gocd-jre',
      'tar -xf /tmp/jre.tar.gz -C /gocd-jre --strip 1',
      'rm -rf /tmp/jre.tar.gz'
    ]
  }

  Map<String, String> getEnvironmentVariables() {
    return [GO_JAVA_HOME: '/gocd-jre']
  }

  boolean isPrivilegedModeSupport() {
    return false
  }

}
