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

package com.thoughtworks.go.build.docker

import com.thoughtworks.go.build.Architecture
import com.thoughtworks.go.build.OperatingSystem
import org.gradle.api.Project

enum Distro implements DistroBehavior {

  alpine {
    @Override
    List<DistroVersion> getSupportedVersions() {
      [ // See https://endoflife.date/alpine
        new DistroVersion(version: '3', releaseName: '3', eolDate: parseDate('2099-01-01')),
      ]
    }

    @Override
    boolean isContinuousRelease() {
      true
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      ['apk --no-cache upgrade']
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      [
        'adduser -D -u ${UID} -s /bin/bash -G root go'
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      [
        // procps is needed for tanuki wrapper shell script
        'apk add --no-cache git openssh-client bash curl procps'
      ]
    }

    @Override
    String getMultiStageInputImage() {
      "frolvlad/alpine-glibc:alpine-3"
    }

    @Override
    String getMultiStageInputDirectory() {
      "/usr/glibc-compat"
    }

    @Override
    List<String> getInstallJavaCommands(Project project) {
      // Tanuki Wrapper currently requires glibc, which is not available in Alpine (which is a musl libc distro). See https://github.com/gocd/gocd/issues/11355
      // for a discussion of this problem. To workaround this, use glibc built within https://hub.docker.com/r/frolvlad/alpine-glibc from source at
      // https://github.com/Docker-Hub-frolvlad/docker-alpine-glibc since he original project at https://github.com/sgerrand/alpine-pkg-glibc is unmaintained.
      //
      // Logic needs to match
      // - package contents from https://github.com/Docker-Hub-frolvlad/docker-alpine-glibc/blob/master/APKBUILD
      // - pre-generated locales from pre-generated locales at https://github.com/Docker-Hub-frolvlad/docker-alpine-glibc/blob/master/Dockerfile
      //
      // Note that this means the JRE used also must be glibc-linked.
      [
        '# install glibc for the Tanuki Wrapper, and use by glibc-linked Adoptium JREs',
        "  GLIBC_DIR=${getMultiStageInputDirectory()}",
        '  GLIBC_LIB=$([ "$(arch)" = "aarch64" ] && echo ld-linux-aarch64.so.1 || echo ld-linux-x86-64.so.2)',
        '  ln -s ${GLIBC_DIR}/lib/${GLIBC_LIB} /lib/${GLIBC_LIB}',
        '  mkdir -p /lib64 && ln -s ${GLIBC_DIR}/lib/${GLIBC_LIB} /lib64/${GLIBC_LIB}',
        '  ln -s ${GLIBC_DIR}/etc/ld.so.cache /etc/ld.so.cache',
        '  echo "export LANG=C.UTF-8" > /etc/profile.d/locale.sh',
        '  ${GLIBC_DIR}/sbin/ldconfig',
        '# end installing glibc',
      ] + super.getInstallJavaCommands(project)
    }

    @Override
    Map<String, String> getEnvironmentVariables(DistroVersion v) {
      // See pre-generated locales at https://github.com/Docker-Hub-frolvlad/docker-alpine-glibc/blob/master/Dockerfile
      ['LANG': 'C.UTF-8'] + super.getEnvironmentVariables(v)
    }
  },

  wolfi {
    @Override
    List<DistroVersion> getSupportedVersions() {
      [
         new DistroVersion(version: 'latest', releaseName: 'latest', eolDate: parseDate('2099-01-01'))
      ]
    }

    @Override
    boolean isContinuousRelease() {
      true
    }

    @Override
    String getBaseImageLocation(DistroVersion distroVersion) {
      "cgr.dev/chainguard/wolfi-base"
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      ['apk --no-cache upgrade']
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      [
        'adduser -D -u ${UID} -s /bin/bash -G root go'
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      [
        // procps is needed for tanuki wrapper shell script
        'apk add --no-cache git openssh-client bash curl procps glibc-locale-en'
      ]
    }
  },

  almalinux {
    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      [
        "echo 'fastestmirror=1' >> /etc/dnf/dnf.conf",
        "echo 'install_weak_deps=False' >> /etc/dnf/dnf.conf",
        "microdnf upgrade -y",
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      [
        "microdnf install -y git-core openssh-clients bash unzip curl-minimal procps-ng coreutils-single glibc-langpack-en tar",
        "microdnf clean all",
        "rm -rf var/cache/dnf",
      ]
    }
    @Override
    List<DistroVersion> getSupportedVersions() {
      [ // See https://endoflife.date/almalinux
        new DistroVersion(version: '9', releaseName: '9-minimal', eolDate: parseDate('2032-05-31')),
      ]
    }
  },

  debian {
    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      [
        'DEBIAN_FRONTEND=noninteractive apt-get update',
        'DEBIAN_FRONTEND=noninteractive apt-get upgrade -y',
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      [
        'DEBIAN_FRONTEND=noninteractive apt-get install -y git-core openssh-client bash unzip curl ca-certificates locales procps coreutils',
        'DEBIAN_FRONTEND=noninteractive apt-get clean all',
        'rm -rf /var/lib/apt/lists/*',
        'echo \'en_US.UTF-8 UTF-8\' > /etc/locale.gen && /usr/sbin/locale-gen'
      ]
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      [ // See https://endoflife.date/debian
        new DistroVersion(version: '11', releaseName: '11-slim', eolDate: parseDate('2026-08-31')),
        new DistroVersion(version: '12', releaseName: '12-slim', eolDate: parseDate('2028-06-10')),
      ]
    }
  },

  ubuntu {
    @Override
    Set<Architecture> getSupportedArchitectures() {
      debian.supportedArchitectures
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      debian.getBaseImageUpdateCommands(v)
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      debian.getInstallPrerequisitesCommands(v)
    }

    List<String> getCreateUserAndGroupCommands() {
      [
        '(userdel --remove --force ubuntu || true)',
      ] + super.getCreateUserAndGroupCommands()
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      [ // See https://endoflife.date/ubuntu "Maintenance & Security Support"
        new DistroVersion(version: '20.04', releaseName: '20.04', eolDate: parseDate('2025-05-31')),
        new DistroVersion(version: '22.04', releaseName: '22.04', eolDate: parseDate('2027-04-01')),
        new DistroVersion(version: '24.04', releaseName: '24.04', eolDate: parseDate('2029-04-25')),
      ]
    }
  },

  docker {
    @Override
    OperatingSystem getOperatingSystem() {
      alpine.getOperatingSystem()
    }

    @Override
    boolean isPrivilegedModeSupport() {
      true
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      [
        new DistroVersion(version: 'dind', releaseName: 'dind', eolDate: parseDate('2099-01-01'))
      ]
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      alpine.getBaseImageUpdateCommands(v)
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      alpine.getCreateUserAndGroupCommands() +
        [
          'adduser go docker' // Add user to the docker group used to control access to the Docker daemon unix socket
        ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      alpine.getInstallPrerequisitesCommands(v) +
        [
          'apk add --no-cache sudo',
        ]
    }

    @Override
    String getMultiStageInputImage() {
      alpine.getMultiStageInputImage()
    }

    @Override
    String getMultiStageInputDirectory() {
      alpine.getMultiStageInputDirectory()
    }

    @Override
    List<String> getInstallJavaCommands(Project project) {
      alpine.getInstallJavaCommands(project)
    }

    @Override
    Map<String, String> getEnvironmentVariables(DistroVersion v) {
      alpine.getEnvironmentVariables(v)
    }

    @Override
    List<List<String>> getAdditionalVerifyCommands() {
      [
        ['docker', 'run', 'hello-world']
      ]
    }
  }

  static Date parseDate(String date) {
    Date.parse("yyyy-MM-dd", date)
  }

  GString projectName(DistroVersion v) {
    "${name()}-${v.version}"
  }
}
