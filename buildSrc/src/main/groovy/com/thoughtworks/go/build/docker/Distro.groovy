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
      return [ // See https://endoflife.date/alpine
        new DistroVersion(version: '3', releaseName: '3', eolDate: parseDate('2099-01-01')),
      ]
    }

    @Override
    boolean isContinuousRelease() {
      return true
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      return ['apk --no-cache upgrade']
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      return [
        'adduser -D -u ${UID} -s /bin/bash -G root go'
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return [
        // procps is needed for tanuki wrapper shell script
        'apk add --no-cache git openssh-client bash curl procps'
      ]
    }

    @Override
    List<String> getInstallJavaCommands(Project project) {
      // Originally copied verbatim from https://github.com/AdoptOpenJDK/openjdk-docker/blob/436253bad9e494ea0043da22fca197e6055a538a/15/jdk/alpine/Dockerfile.hotspot.releases.slim#L24-L55
      // Subsequent to this, Adoptium have moved to use of native Alpine/musl libc builds for JDK 16/17 onwards.
      // More detail at https://github.com/adoptium/temurin-build/issues/2688 and https://github.com/adoptium/containers/issues/1
      // Unfortunately, these do not work for GoCD due to the Tanuki Wrapper which does not currently work with musl libc,
      // nor alternate compatibility layers such as libc6-compat or gcompat.
      return [
        '# install glibc for the Tanuki Wrapper, and use by glibc-linked Adoptium JREs',
        '  apk add --no-cache tzdata --virtual .build-deps curl',
        '  GLIBC_VER="2.34-r0"',
        '  ALPINE_GLIBC_REPO="https://github.com/sgerrand/alpine-pkg-glibc/releases/download"',
        '  curl -LfsS https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub -o /etc/apk/keys/sgerrand.rsa.pub',
        '  SGERRAND_RSA_SHA256="823b54589c93b02497f1ba4dc622eaef9c813e6b0f0ebbb2f771e32adf9f4ef2"',
        '  echo "${SGERRAND_RSA_SHA256} */etc/apk/keys/sgerrand.rsa.pub" | sha256sum -c -',
        '  curl -LfsS ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-${GLIBC_VER}.apk > /tmp/glibc-${GLIBC_VER}.apk',
        '  apk add --no-cache --force-overwrite /tmp/glibc-${GLIBC_VER}.apk',
        '  curl -LfsS ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-bin-${GLIBC_VER}.apk > /tmp/glibc-bin-${GLIBC_VER}.apk',
        '  apk add --no-cache /tmp/glibc-bin-${GLIBC_VER}.apk',
        '  curl -Ls ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-i18n-${GLIBC_VER}.apk > /tmp/glibc-i18n-${GLIBC_VER}.apk',
        '  apk add --no-cache /tmp/glibc-i18n-${GLIBC_VER}.apk',
        '  /usr/glibc-compat/bin/localedef --force --inputfile POSIX --charmap UTF-8 "$LANG" || true',
        '  echo "export LANG=$LANG" > /etc/profile.d/locale.sh',
        '  apk del --purge .build-deps glibc-i18n',
        '  rm -rf /tmp/*.apk /var/cache/apk/*',
        '# end installing glibc',
      ] + super.getInstallJavaCommands(project)
    }
  },

  wolfi {
    @Override
    Set<Architecture> getSupportedArchitectures() {
      [Architecture.x64, Architecture.aarch64]
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
         new DistroVersion(version: 'latest', releaseName: 'latest', eolDate: parseDate('2099-01-01'))
      ]
    }

    @Override
    boolean isContinuousRelease() {
      return true
    }

    @Override
    String getBaseImageLocation(DistroVersion distroVersion) {
      "cgr.dev/chainguard/wolfi-base"
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      return ['apk --no-cache upgrade']
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      return [
        'adduser -D -u ${UID} -s /bin/bash -G root go'
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return [
        // procps is needed for tanuki wrapper shell script
        'apk add --no-cache git openssh-client bash curl procps glibc-locale-en'
      ]
    }
  },

  almalinux {
    @Override
    Set<Architecture> getSupportedArchitectures() {
      [Architecture.x64, Architecture.aarch64]
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      return [
        "echo 'fastestmirror=1' >> /etc/dnf/dnf.conf",
        "echo 'install_weak_deps=False' >> /etc/dnf/dnf.conf",
        "microdnf upgrade -y",
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return [
        "microdnf install -y git-core openssh-clients bash unzip curl-minimal procps-ng coreutils-single glibc-langpack-en tar",
        "microdnf clean all",
        "rm -rf var/cache/dnf",
      ]
    }
    @Override
    List<DistroVersion> getSupportedVersions() {
      return [ // See https://endoflife.date/almalinux
        new DistroVersion(version: '9', releaseName: '9-minimal', eolDate: parseDate('2032-05-31')),
      ]
    }
  },

  debian {
    @Override
    Set<Architecture> getSupportedArchitectures() {
      [Architecture.x64, Architecture.aarch64]
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      return [
        'DEBIAN_FRONTEND=noninteractive apt-get update',
        'DEBIAN_FRONTEND=noninteractive apt-get upgrade -y',
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return [
        'DEBIAN_FRONTEND=noninteractive apt-get install -y git-core openssh-client bash unzip curl ca-certificates locales procps coreutils',
        'DEBIAN_FRONTEND=noninteractive apt-get clean all',
        'rm -rf /var/lib/apt/lists/*',
        'echo \'en_US.UTF-8 UTF-8\' > /etc/locale.gen && /usr/sbin/locale-gen'
      ]
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [ // See https://endoflife.date/debian
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
      return debian.getBaseImageUpdateCommands(v)
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return debian.getInstallPrerequisitesCommands(v)
    }

    List<String> getCreateUserAndGroupCommands() {
      return [
        '(userdel --remove --force ubuntu || true)',
      ] + super.getCreateUserAndGroupCommands()
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [ // See https://endoflife.date/ubuntu "Maintenance & Security Support"
        new DistroVersion(version: '20.04', releaseName: '20.04', eolDate: parseDate('2025-04-02')),
        new DistroVersion(version: '22.04', releaseName: '22.04', eolDate: parseDate('2027-04-01')),
        new DistroVersion(version: '24.04', releaseName: '24.04', eolDate: parseDate('2029-04-25')),
      ]
    }
  },

  docker {
    @Override
    OperatingSystem getOperatingSystem() {
      return alpine.getOperatingSystem()
    }

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
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      return alpine.getBaseImageUpdateCommands(v)
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      return alpine.getCreateUserAndGroupCommands() +
        [
          'adduser go docker' // Add user to the docker group used to control access to the Docker daemon unix socket
        ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return alpine.getInstallPrerequisitesCommands(v) +
        [
          'apk add --no-cache sudo',
        ]
    }

    @Override
    List<String> getInstallJavaCommands(Project project) {
      return alpine.getInstallJavaCommands(project)
    }

    @Override
    Map<String, String> getEnvironmentVariables(DistroVersion v) {
      return alpine.getEnvironmentVariables(v)
    }

    @Override
    List<List<String>> getAdditionalVerifyCommands() {
      return [
        ['docker', 'run', 'hello-world']
      ]
    }
  }

  static Date parseDate(String date) {
    return Date.parse("yyyy-MM-dd", date)
  }

  GString projectName(DistroVersion v) {
    return "${name()}-${v.version}"
  }
}
