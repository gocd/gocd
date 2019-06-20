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
        'adduser -D -u ${UID} -s /bin/bash -G root go'
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands() {
      return [
        'apk --no-cache upgrade',
        // procps is needed for tanuki wrapper shell script
        'apk add --no-cache nss git mercurial subversion openssh-client bash curl procps'
      ]
    }

    @Override
    List<String> getInstallJavaCommands(Project project) {
      // Copied verbatim from https://github.com/AdoptOpenJDK/openjdk-docker/blob/ce8b120411b131e283106ab89ea5921ebb1d1759/8/jdk/alpine/Dockerfile.hotspot.releases.slim#L24-L54
      return [
        '# install glibc and zlib for adoptopenjdk',
        '# See https://github.com/AdoptOpenJDK/openjdk-docker/blob/ce8b120411b131e283106ab89ea5921ebb1d1759/8/jdk/alpine/Dockerfile.hotspot.releases.slim#L24-L54',
        '  apk add --no-cache --virtual .build-deps curl binutils',
        '  GLIBC_VER="2.29-r0"',
        '  ALPINE_GLIBC_REPO="https://github.com/sgerrand/alpine-pkg-glibc/releases/download"',
        '  GCC_LIBS_URL="https://archive.archlinux.org/packages/g/gcc-libs/gcc-libs-8.2.1%2B20180831-1-x86_64.pkg.tar.xz"',
        '  GCC_LIBS_SHA256=e4b39fb1f5957c5aab5c2ce0c46e03d30426f3b94b9992b009d417ff2d56af4d',
        '  ZLIB_URL="https://archive.archlinux.org/packages/z/zlib/zlib-1%3A1.2.11-3-x86_64.pkg.tar.xz"',
        '  ZLIB_SHA256=17aede0b9f8baa789c5aa3f358fbf8c68a5f1228c5e6cba1a5dd34102ef4d4e5',
        '  curl -LfsS https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub -o /etc/apk/keys/sgerrand.rsa.pub',
        '  SGERRAND_RSA_SHA256="823b54589c93b02497f1ba4dc622eaef9c813e6b0f0ebbb2f771e32adf9f4ef2"',
        '  echo "${SGERRAND_RSA_SHA256} */etc/apk/keys/sgerrand.rsa.pub" | sha256sum -c -',
        '  curl -LfsS ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-${GLIBC_VER}.apk > /tmp/glibc-${GLIBC_VER}.apk',
        '  apk add /tmp/glibc-${GLIBC_VER}.apk',
        '  curl -LfsS ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-bin-${GLIBC_VER}.apk > /tmp/glibc-bin-${GLIBC_VER}.apk',
        '  apk add /tmp/glibc-bin-${GLIBC_VER}.apk',
        '  curl -Ls ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-i18n-${GLIBC_VER}.apk > /tmp/glibc-i18n-${GLIBC_VER}.apk',
        '  apk add /tmp/glibc-i18n-${GLIBC_VER}.apk',
        '  /usr/glibc-compat/bin/localedef --force --inputfile POSIX --charmap UTF-8 "$LANG" || true',
        '  echo "export LANG=$LANG" > /etc/profile.d/locale.sh',
        '  curl -LfsS ${GCC_LIBS_URL} -o /tmp/gcc-libs.tar.xz',
        '  echo "${GCC_LIBS_SHA256} */tmp/gcc-libs.tar.xz" | sha256sum -c -',
        '  mkdir /tmp/gcc',
        '  tar -xf /tmp/gcc-libs.tar.xz -C /tmp/gcc',
        '  mv /tmp/gcc/usr/lib/libgcc* /tmp/gcc/usr/lib/libstdc++* /usr/glibc-compat/lib',
        '  strip /usr/glibc-compat/lib/libgcc_s.so.* /usr/glibc-compat/lib/libstdc++.so*',
        '  curl -LfsS ${ZLIB_URL} -o /tmp/libz.tar.xz',
        '  echo "${ZLIB_SHA256} */tmp/libz.tar.xz" | sha256sum -c -',
        '  mkdir /tmp/libz',
        '  tar -xf /tmp/libz.tar.xz -C /tmp/libz',
        '  mv /tmp/libz/usr/lib/libz.so* /usr/glibc-compat/lib',
        '  apk del --purge .build-deps glibc-i18n',
        '  rm -rf /tmp/*.apk /tmp/gcc /tmp/gcc-libs.tar.xz /tmp/libz /tmp/libz.tar.xz /var/cache/apk/*',
        '# end installing adoptopenjre ',
      ] + super.getInstallJavaCommands(project)
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
        BASH_ENV: '/opt/rh/rh-git29/enable',
        ENV     : '/opt/rh/rh-git29/enable'
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
        'apt-get install -y git subversion mercurial openssh-client bash unzip curl locales procps sysvinit-utils coreutils',
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
      return alpine.getCreateUserAndGroupCommands()
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
      return alpine.getEnvironmentVariables()
    }
  }

  static Date parseDate(String date) {
    return Date.parse("yyyy-MM-dd", date)
  }

  GString projectName(DistroVersion distroVersion) {
    return "${name()}-${distroVersion.version}"
  }
}
