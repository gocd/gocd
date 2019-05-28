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

import org.gradle.api.Project

enum Distro implements DistroBehavior {

  alpine{
    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '3.7', releaseName: '3.7', eolDate: parseDate('2019-11-01')),
        new DistroVersion(version: '3.8', releaseName: '3.8', eolDate: parseDate('2020-05-01')),
        new DistroVersion(version: '3.9', releaseName: '3.9', eolDate: parseDate('2021-01-01'))
      ]
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      return [
        'addgroup -g ${GID} go',
        'adduser -D -u ${UID} -s /bin/bash -G go go'
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands() {
      return [
        'apk --no-cache upgrade',
        'apk add --no-cache nss git mercurial subversion openssh-client bash curl'
      ]
    }

    @Override
    List<String> getInstallJavaCommands(Project project) {
      return [
        'apk add --no-cache openjdk8-jre-base'
      ]
    }

    @Override
    Map<String, String> getEnvironmentVariables() {
      return [:]
    }
  },

  centos{
    @Override
    List<String> getInstallPrerequisitesCommands() {
      return [
        'yum update -y',
        'yum install -y mercurial subversion openssh-clients bash unzip curl',
        "yum install --assumeyes centos-release-scl",
        "yum install --assumeyes rh-git29",
        "cp /opt/rh/rh-git29/enable /etc/profile.d/rh-git29.sh",
        "yum clean all"
      ]
    }

    @Override
    Map<String, String> getEnvironmentVariables() {
      return super.getEnvironmentVariables() + [
        BASH_ENV      : '/opt/rh/rh-git29/enable',
        ENV           : '/opt/rh/rh-git29/enable'
      ]
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '6', releaseName: '6', eolDate: parseDate('2020-11-01')),
        new DistroVersion(version: '7', releaseName: '7', eolDate: parseDate('2024-06-01'))
      ]
    }
  },

  fedora{
    @Override
    List<String> getInstallPrerequisitesCommands() {
      return [
        'yum update -y',
        'yum install -y git mercurial subversion openssh-clients bash unzip curl',
        'yum clean all'
      ]
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      // Fedora has 13 months for EOL
      return [
        // approximate date - 1 year from release date, check when the build fails
        new DistroVersion(version: '29', releaseName: '29', eolDate: parseDate('2019-11-30')),
        new DistroVersion(version: '30', releaseName: '30', eolDate: parseDate('2020-06-01')),
      ]
    }
  },

  debian{
    @Override
    List<String> getInstallPrerequisitesCommands() {
      return [
        'apt-get update',
        'apt-get install -y git subversion mercurial openssh-client bash unzip curl locales',
        'apt-get autoclean',
        'echo \'en_US.UTF-8 UTF-8\' > /etc/locale.gen && /usr/sbin/locale-gen'
      ]
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '8', releaseName: 'jessie', eolDate: parseDate('2020-06-30')),
        new DistroVersion(version: '9', releaseName: 'stretch', eolDate: parseDate('2022-06-30'))
      ]
    }
  },

  ubuntu{
    @Override
    List<String> getInstallPrerequisitesCommands() {
      return debian.getInstallPrerequisitesCommands()
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '16.04', releaseName: 'xenial', eolDate: parseDate('2021-04-01')),
        new DistroVersion(version: '18.04', releaseName: 'bionic', eolDate: parseDate('2023-04-01'))
      ]
    }
  },

  docker{
    @Override
    boolean isPrivilegedModeSupport() {
      return true
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: 'dind', releaseName: 'dind', eolDate: parseDate('2099-01-01'))
      ]
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      def commands = alpine.getCreateUserAndGroupCommands()
      commands.add('addgroup go root')

      return commands
    }

    @Override
    List<String> getInstallPrerequisitesCommands() {
      return alpine.getInstallPrerequisitesCommands()
    }


    @Override
    List<String> getInstallJavaCommands(Project project) {
      return alpine.getInstallJavaCommands(project)
    }

    @Override
    Map<String, String> getEnvironmentVariables() {
      return [:]
    }
  }

  static Date parseDate(String date) {
    return Date.parse("yyyy-MM-dd", date)
  }

  GString projectName(DistroVersion distroVersion) {
    return "${name()}-${distroVersion.version}"
  }
}
