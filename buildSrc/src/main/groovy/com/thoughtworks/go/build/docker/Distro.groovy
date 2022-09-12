/*
 * Copyright 2022 Thoughtworks, Inc.
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

import com.thoughtworks.go.build.OperatingSystem
import org.gradle.api.Project

enum Distro implements DistroBehavior {

  alpine{
    @Override
    OperatingSystem getOperatingSystem() {
      OperatingSystem.linux
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '3.13', releaseName: '3.13', eolDate: parseDate('2022-11-01'), continueToBuild: true),
        new DistroVersion(version: '3.14', releaseName: '3.14', eolDate: parseDate('2023-05-01')),
        new DistroVersion(version: '3.15', releaseName: '3.15', eolDate: parseDate('2023-11-01')),
        new DistroVersion(version: '3.16', releaseName: '3.16', eolDate: parseDate('2024-05-23')),
      ]
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
        'apk --no-cache upgrade',
        // procps is needed for tanuki wrapper shell script
        'apk add --no-cache nss git mercurial subversion openssh-client bash curl procps'
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
        '# install glibc/gcc-libs/zlib for the Tanuki Wrapper, and use by glibc-linked Adoptium JREs',
        '  apk add --no-cache tzdata --virtual .build-deps curl binutils zstd',
        '  GLIBC_VER="2.34-r0"',
        '  ALPINE_GLIBC_REPO="https://github.com/sgerrand/alpine-pkg-glibc/releases/download"',
        '  GCC_LIBS_URL="https://archive.archlinux.org/packages/g/gcc-libs/gcc-libs-10.2.0-6-x86_64.pkg.tar.zst"',
        '  GCC_LIBS_SHA256="e33b45e4a10ef26259d6acf8e7b5dd6dc63800641e41eb67fa6588d061f79c1c"',
        '  ZLIB_URL="https://archive.archlinux.org/packages/z/zlib/zlib-1%3A1.2.12-2-x86_64.pkg.tar.zst"',
        '  ZLIB_SHA256=506577ab283c0e5dafaa61d645994c38560234a871fbc9ef2b45327a9a965d66',
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
        '  curl -LfsS ${GCC_LIBS_URL} -o /tmp/gcc-libs.tar.zst',
        '  echo "${GCC_LIBS_SHA256} */tmp/gcc-libs.tar.zst" | sha256sum -c -',
        '  mkdir /tmp/gcc',
        '  zstd -d /tmp/gcc-libs.tar.zst --output-dir-flat /tmp',
        '  tar -xf /tmp/gcc-libs.tar -C /tmp/gcc',
        '  mv /tmp/gcc/usr/lib/libgcc* /tmp/gcc/usr/lib/libstdc++* /usr/glibc-compat/lib',
        '  strip /usr/glibc-compat/lib/libgcc_s.so.* /usr/glibc-compat/lib/libstdc++.so*',
        '  curl -LfsS ${ZLIB_URL} -o /tmp/libz.tar.zst',
        '  echo "${ZLIB_SHA256} */tmp/libz.tar.zst" | sha256sum -c -',
        '  mkdir /tmp/libz',
        '  zstd -d /tmp/libz.tar.zst --output-dir-flat /tmp',
        '  tar -xf /tmp/libz.tar -C /tmp/libz',
        '  mv /tmp/libz/usr/lib/libz.so* /usr/glibc-compat/lib',
        '  apk del --purge .build-deps glibc-i18n',
        '  rm -rf /tmp/*.apk /tmp/gcc /tmp/gcc-libs.tar* /tmp/libz /tmp/libz.tar* /var/cache/apk/*',
        '# end installing glibc/gcc-libs/zlib',
      ] + super.getInstallJavaCommands(project)
    }
  },

  centos{
    @Override
    String getBaseImageRegistry(DistroVersion v) {
      v.lessThan(8) ? super.baseImageRegistry : "quay.io/centos"
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      def commands = ['yum update -y']

      String git = gitPackageFor(v)
      commands.add(
        "yum install -y ${git} mercurial subversion openssh-clients bash unzip procps" +
          (v.lessThan(8) ? ' sysvinit-tools coreutils' : ' procps-ng coreutils-single') +
          (v.lessThan(9) ? ' curl' : ' curl-minimal')
      )

      if (v.lessThan(8)) {
        commands.add("cp /opt/rh/${git}/enable /etc/profile.d/${git}.sh")
      }

      commands.add('yum clean all')

      return commands
    }

    String gitPackageFor(DistroVersion v) {
      return v.lessThan(8) ? "rh-git227" : "git"
    }

    @Override
    Map<String, String> getEnvironmentVariables(DistroVersion v) {
      def vars = super.getEnvironmentVariables(v)

      if (v.lessThan(8)) {
        String git = gitPackageFor(v)
        return vars + [
          BASH_ENV: "/opt/rh/${git}/enable",
          ENV     : "/opt/rh/${git}/enable"
        ] as Map<String, String>
      } else {
        return vars
      }
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '7', releaseName: '7', eolDate: parseDate('2024-06-01'), installPrerequisitesCommands: ['yum install --assumeyes centos-release-scl-rh']),
        new DistroVersion(version: '8', releaseName: 'stream8', eolDate: parseDate('2024-05-31'), installPrerequisitesCommands: ['yum install --assumeyes glibc-langpack-en']),
        new DistroVersion(version: '9', releaseName: 'stream9', eolDate: parseDate('2027-05-31'), installPrerequisitesCommands: ['yum install --assumeyes glibc-langpack-en epel-release']),
      ]
    }
  },

  debian{
    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return [
        'apt-get update',
        'apt-get install -y git subversion mercurial openssh-client bash unzip curl ca-certificates locales procps sysvinit-utils coreutils',
        'apt-get clean all',
        'rm -rf /var/lib/apt/lists/*',
        'echo \'en_US.UTF-8 UTF-8\' > /etc/locale.gen && /usr/sbin/locale-gen'
      ]
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '10', releaseName: 'buster-slim', eolDate: parseDate('2024-06-01')),
        new DistroVersion(version: '11', releaseName: 'bullseye-slim', eolDate: parseDate('2026-08-15')),
      ]
    }
  },

  ubuntu{
    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion v) {
      return debian.getInstallPrerequisitesCommands(v)
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '18.04', releaseName: 'bionic', eolDate: parseDate('2023-04-01')),
        new DistroVersion(version: '20.04', releaseName: 'focal', eolDate: parseDate('2030-04-01')),
        new DistroVersion(version: '22.04', releaseName: 'jammy', eolDate: parseDate('2032-04-01')),
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
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: 'dind', releaseName: 'dind', eolDate: parseDate('2099-01-01'))
      ]
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
        ]
    }

    @Override
    List<String> getInstallJavaCommands(Project project) {
      return [
        // Workaround for https://github.com/docker-library/docker/commit/75e26edc9ea7fff4aa3212fafa5966f4d6b00022
        // which causes a clash with glibc, which is installed later due to being needed for Tanuki Java Wrapper (and
        // thus used by the particular Adoptium builds we are using Alpine Adoptium builds seemingly can't co-exist happily).
        // We could avoid doing this once https://github.com/containerd/containerd/issues/5824 is fixed and makes its
        // way to the relevant docker:dind image version.
        'apk del --purge libc6-compat'
      ] + alpine.getInstallJavaCommands(project)
    }

    @Override
    Map<String, String> getEnvironmentVariables(DistroVersion v) {
      return alpine.getEnvironmentVariables(v)
    }
  }

  static Date parseDate(String date) {
    return Date.parse("yyyy-MM-dd", date)
  }

  GString projectName(DistroVersion v) {
    return "${name()}-${v.version}"
  }
}
