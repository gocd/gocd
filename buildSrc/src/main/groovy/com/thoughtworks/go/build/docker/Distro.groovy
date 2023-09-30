/*
 * Copyright 2023 Thoughtworks, Inc.
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

  alpine{
    @Override
    List<DistroVersion> getSupportedVersions() {
      return [ // See https://endoflife.date/alpine
        new DistroVersion(version: '3.15', releaseName: '3.15', eolDate: parseDate('2023-11-01')),
        new DistroVersion(version: '3.16', releaseName: '3.16', eolDate: parseDate('2024-05-23')),
        new DistroVersion(version: '3.17', releaseName: '3.17', eolDate: parseDate('2024-11-22')),
        new DistroVersion(version: '3.18', releaseName: '3.18', eolDate: parseDate('2025-05-09')),
      ]
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
        'apk add --no-cache git mercurial subversion openssh-client bash curl procps'
      ]
    }

    @Override
    List<String> getInstallJavaCommands(Project project) {
      // Originally copied verbatim from https://github.com/AdoptOpenJDK/openjdk-docker/blob/436253bad9e494ea0043da22fca197e6055a538a/15/jdk/alpine/Dockerfile.hotspot.releases.slim#L24-L55
      // Subsequent to this, Adoptium have moved to use of native Alpine/musl libc builds for JDK 16/17 onwards.
      // More detail at https://github.com/adoptium/temurin-build/issues/2688 and https://github.com/adoptium/containers/issues/1
      // Unfortunately, these do not work for GoCD due to the Tanuki Wrapper which does not currently work with musl libc,
      // nor alternate compatibility layers such as libc6-compat or gcompat.
      //
      // zlib/libz.so.1 is required by the JRE, and we need a glibc-linked version. Can probably be latest available version.
      return [
        '# install glibc/zlib for the Tanuki Wrapper, and use by glibc-linked Adoptium JREs',
        '  apk add --no-cache tzdata --virtual .build-deps curl binutils zstd',
        '  GLIBC_VER="2.34-r0"',
        '  ALPINE_GLIBC_REPO="https://github.com/sgerrand/alpine-pkg-glibc/releases/download"',
        '  ZLIB_URL="https://archive.archlinux.org/packages/z/zlib/zlib-1%3A1.3-1-x86_64.pkg.tar.zst"',
        '  ZLIB_SHA256=8a9b1e5e354197828e74860b846a951cd981833d1b07ae182a539b0524b6ef3a',
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
        '  curl -LfsS ${ZLIB_URL} -o /tmp/libz.tar.zst',
        '  echo "${ZLIB_SHA256} */tmp/libz.tar.zst" | sha256sum -c -',
        '  mkdir /tmp/libz',
        '  zstd -d /tmp/libz.tar.zst --output-dir-flat /tmp',
        '  tar -xf /tmp/libz.tar -C /tmp/libz',
        '  mv /tmp/libz/usr/lib/libz.so* /usr/glibc-compat/lib',
        '  apk del --purge .build-deps glibc-i18n',
        '  rm -rf /tmp/*.apk /tmp/libz /tmp/libz.tar* /var/cache/apk/*',
        '# end installing glibc/zlib',
      ] + super.getInstallJavaCommands(project)
    }
  },

  centos{
    @Override
    Set<Architecture> getSupportedArchitectures() {
      [Architecture.x64, Architecture.aarch64]
    }

    @Override
    String getBaseImageRegistry(DistroVersion v) {
      "quay.io/centos"
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      return [
        "echo 'fastestmirror=1' >> /etc/dnf/dnf.conf",
        "echo 'install_weak_deps=False' >> /etc/dnf/dnf.conf",
        "${pkgFor(v)} upgrade -y",
        "${pkgFor(v)} install -y shadow-utils",
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return [
        "${pkgFor(v)} install -y git-core mercurial subversion openssh-clients bash unzip procps-ng coreutils-single glibc-langpack-en ${v.lessThan(9) ? ' curl' : ' curl-minimal'}",
        "${pkgFor(v)} clean all",
        "rm -rf /var/cache/yum /var/cache/dnf",
      ]
    }

    private String pkgFor(DistroVersion v) {
      v.lessThan(9) ? 'dnf' : 'microdnf'
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [ // See https://endoflife.date/centos
        new DistroVersion(version: '8', releaseName: 'stream8', eolDate: parseDate('2024-05-31')),
        new DistroVersion(version: '9', releaseName: 'stream9-minimal', eolDate: parseDate('2027-05-31'), installPrerequisitesCommands: ['microdnf install -y tar tzdata epel-release epel-next-release']),
      ]
    }
  },

  debian{
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
        'DEBIAN_FRONTEND=noninteractive apt-get install -y git-core subversion mercurial openssh-client bash unzip curl ca-certificates locales procps sysvinit-utils coreutils',
        'DEBIAN_FRONTEND=noninteractive apt-get clean all',
        'rm -rf /var/lib/apt/lists/*',
        'echo \'en_US.UTF-8 UTF-8\' > /etc/locale.gen && /usr/sbin/locale-gen'
      ]
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [ // See https://endoflife.date/debian
        new DistroVersion(version: '10', releaseName: 'buster-slim', eolDate: parseDate('2024-06-01')),
        new DistroVersion(version: '11', releaseName: 'bullseye-slim', eolDate: parseDate('2026-06-30')),
        new DistroVersion(version: '12', releaseName: 'bookworm-slim', eolDate: parseDate('2028-06-10')),
      ]
    }
  },

  ubuntu{
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

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [ // See https://endoflife.date/ubuntu "Maintenance & Security Support"
        new DistroVersion(version: '20.04', releaseName: 'focal', eolDate: parseDate('2025-04-02')),
        new DistroVersion(version: '22.04', releaseName: 'jammy', eolDate: parseDate('2027-04-01')),
      ]
    }
  },

  docker{
    @Override
    OperatingSystem getOperatingSystem() {
      return alpine.getOperatingSystem()
    }

    @Override
    boolean isPrivilegedModeSupport() {
      return true
    }

    @Override
    List<String> getBaseImageUpdateCommands(DistroVersion v) {
      return alpine.getBaseImageUpdateCommands(v)
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      return alpine.getCreateUserAndGroupCommands()
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return alpine.getInstallPrerequisitesCommands(v) +
        [
          'apk add --no-cache sudo',
          'rm -rf /lib/libudev.so*', // btrfs is installed by Docker, but requires eudev-libs, which causes issues with OSHI JNA libary on Alpine with glibc and JVM crashes. Dont think we need udev as it's only needed by btrfs for multipath support.
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
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: 'dind', releaseName: 'dind', eolDate: parseDate('2099-01-01'))
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
