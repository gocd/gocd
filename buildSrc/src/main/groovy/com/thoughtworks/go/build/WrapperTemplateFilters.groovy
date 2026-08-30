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

/**
 * Mixes the line filters for the Tanuki Wrapper's template files (src/bin/App.sh.in,
 * src/bin/*.bat.in and src/conf/wrapper.conf.in) into {@link InstallerType}. The generic zip
 * and the Linux packages lay the same templates out against different filesystem structures,
 * expressed via the {@link InstallerLayout} each packaging flavour passes in.
 */
trait WrapperTemplateFilters {

  abstract String getBaseName()

  abstract String getJarFileName()

  abstract String getLogFileName()

  abstract boolean getAllowPassthrough()

  abstract Map<String, String> getAdditionalEnvVars()

  abstract List<String> getJvmArgs()

  // Work around Tanuki 3.7.2 App.sh.in regressions (revisit on next wrapper upgrade):
  // - On platforms marked FALLBACK_32=false (macOS, Linux aarch64/ppcle/s390x) the script skips
  //   the only block that detects the arch-suffixed delta-pack binaries, leaving no usable
  //   wrapper. Detect unconditionally, using FALLBACK_32 only to skip the 32-bit license check
  //   (always unnecessary for GoCD).
  // - On Apple Silicon the script otherwise selects the x86_64 wrapper binary (Rosetta) for
  //   non-launch commands, or for launch when the JVM is x86_64. Skip the JVM architecture
  //   probing so the arm binary is always used.
  private static final Map<String, String> TANUKI_3_7_2_WORKAROUNDS = [
      'if [ "$BIN_BITS" != "32" -a "$FALLBACK_32" = "true" ] ; then':
      'if [ "$BIN_BITS" != "32" ] ; then',

      '    if tryDetectWrapperBinary "$BIN_ARCH" "$BIN_BITS" ; then':
      '    if tryDetectWrapperBinary "$BIN_ARCH" "$BIN_BITS" && [ "$FALLBACK_32" = "true" ] ; then',

      '    if tryDetectWrapperBinary "arm" "$DIST_BITS" || tryDetectWrapperBinary "x86" "$DIST_BITS" ; then':
      '    if false ; then',
  ]

  Closure<String> wrapperShTemplateFilter(InstallerLayout layout, Map<String, String> packageSpecificReplacements = [:]) {
    // App.sh resolves relative paths against its own bin/ directory, one level inside the
    // install dir, rather than against the working directory - so a relative layout expresses
    // the install root as '..'.
    def replacements = [
      'WRAPPER_CMD="./wrapper"':
      'WRAPPER_CMD="../wrapper/wrapper"', // always relative to bin/ dir, never absolute

      'WRAPPER_CONF="../conf/wrapper.conf"':
      "WRAPPER_CONF=\"${layout.installDirBinRelative}/wrapper-config/wrapper.conf\"", // Sometimes absolute for wrapper license matching

      'PIDDIR="."':
      "PIDDIR=\"${layout.workingDir}/run\"",
    ] + TANUKI_3_7_2_WORKAROUNDS + packageSpecificReplacements

    return { String eachLine ->
      eachLine = eachLine
        .replace('@app.name@', baseName)
        .replace('@app.long.name@', baseName)
        .replace('@app.description@', baseName)

      if (eachLine =~ /^#PASS_THROUGH=/ && allowPassthrough) {
        eachLine = 'PASS_THROUGH=true'
      }

      replacements.getOrDefault(eachLine, eachLine)
    }
  }

  Closure<String> wrapperBatTemplateFilter() {
    def replacements = [
      'set _WRAPPER_DIR=':
      'set _WRAPPER_DIR="..\\wrapper"',

      'set _WRAPPER_CONF_DEFAULT="../conf/%_WRAPPER_BASE%.conf"':
      "set _WRAPPER_CONF_DEFAULT=\"..\\wrapper-config\\%_WRAPPER_BASE%.conf\"",
    ]

    return { String eachLine ->
      if (eachLine =~ /^rem set _PASS_THROUGH=/ && allowPassthrough) {
        eachLine = 'set _PASS_THROUGH=true'
      }

      replacements.getOrDefault(eachLine, eachLine)
    }
  }

  Closure<String> wrapperConfTemplateFilter(InstallerLayout layout, Map<String, String> extraEnvVars = [:], List<String> extraJvmArgs = []) {

    Map<String, String> replacements = [
      'wrapper.java.mainclass=org.tanukisoftware.wrapper.WrapperSimpleApp':
      'wrapper.java.mainclass=org.tanukisoftware.wrapper.WrapperJarApp',

      'wrapper.jarfile=../lib/wrapper.jar':
      "wrapper.jarfile=${layout.installPathWorkingRelative('wrapper/wrapper.jar')}",

      '#wrapper.java.classpath.1=':
      "wrapper.java.classpath.1=${layout.installPathWorkingRelative("lib/${jarFileName}")}",

      'wrapper.java.library.path.1=../lib':
      "wrapper.java.library.path.1=${layout.installPathWorkingRelative('wrapper')}",

      'wrapper.app.parameter.1=<YourMainClass>':
      "wrapper.app.parameter.1=${layout.installPathWorkingRelative("lib/${jarFileName}")}",

      'wrapper.logfile=../logs/wrapper.log':
      "wrapper.logfile=${layout.logDir}/${logFileName}",

      // uncomment the lines below to make debugging of installers easier
//      '# wrapper.debug=TRUE':
//      'wrapper.debug=TRUE',
//
//      '#@include.debug':
//      '@include.debug',
//
//      '#wrapper.license.debug=TRUE':
//      'wrapper.license.debug=TRUE',
    ]

    return { String eachLine ->
      eachLine = eachLine
        .replace('@app.name@', baseName)
        .replace('@app.long.name@', baseName)
        .replace('@app.description@', baseName)

      if (eachLine == '@encoding=UTF-8') {
        def newLines = [
          '@encoding=UTF-8',
          "wrapper.working.dir=${layout.workingDir}",
          'wrapper.console_input=DISABLED',
          'wrapper.console.flush=TRUE'
        ]

        (additionalEnvVars + extraEnvVars).forEach { k, v ->
          newLines.add("set.default.${k}=${v}")
        }

        return newLines.join('\n')
      }

      List<String> allJvmArgs = jvmArgs + extraJvmArgs
      if (eachLine == 'wrapper.java.additional.1=' && !allJvmArgs.isEmpty()) {
        def newLines = []

        allJvmArgs.eachWithIndex { String eachArg, int index ->
          newLines << "wrapper.java.additional.${index + 1}=${eachArg}"
        }

        return newLines.join('\n')
      }

      replacements.getOrDefault(eachLine, eachLine)
    }
  }
}
