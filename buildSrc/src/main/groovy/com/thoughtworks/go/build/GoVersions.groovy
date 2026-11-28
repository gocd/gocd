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

import groovy.transform.Immutable
import groovy.transform.ImmutableOptions
import org.gradle.api.provider.ProviderFactory
import org.gradle.util.GradleVersion

import java.time.Duration

import static org.gradle.api.JavaVersion.*

@Immutable
@ImmutableOptions(knownImmutableClasses = [ProviderFactory])
class GoVersions implements Serializable {
  transient ProviderFactory providers

  String goVersion
  String previousVersion
  String nextVersion

  String gitRevision = git('rev-list', 'HEAD', '--max-count=1')
  String gitShortRevision = git('rev-list', 'HEAD', '--max-count=1', '--abbrev-commit')
  String distVersion = git('rev-list', 'HEAD', '--count')

  String getFullVersion() { distVersion ? "${goVersion}-${distVersion}" : goVersion }

  int targetJavaVersion
  int buildJavaVersion
  AdoptiumVersion packagedJavaVersion

  // Overrides keyed by "<os>-<arch>", for when the default version is not (yet) available for a platform
  Map<String, AdoptiumVersion> packagedJavaVersionOverrides = [:]

  AdoptiumVersion packagedJavaVersionFor(OperatingSystem os, Architecture arch) {
    packagedJavaVersionOverrides["${os.name()}-${arch.canonicalName}".toString()] ?: packagedJavaVersion
  }

  List<String> preferredJavaVersions() {
    [VERSION_21, VERSION_25, VERSION_29] // LTS versions
      .findAll { v -> v.ordinal() >= toVersion(targetJavaVersion).ordinal() && v.ordinal() <= toVersion(buildJavaVersion).ordinal() }
      .sort()
      .reverse()
      .collect { v -> v.majorVersion }
  }

  private String git(String... args) {
    providers.exec { commandLine(['git', *args]); ignoreExitValue = true }.standardOutput.asText.get().trim()
  }

  private String toolVersion(String command) {
    try {
      def exec = providers.exec { commandLine(command.split(' ')); ignoreExitValue = true }
      exec.result.get().exitValue == 0 ? exec.standardOutput.asText.get().trim() : "could not determine version!"
    } catch (Exception e) {
      "could not determine version! (${e.message})"
    }
  }

  GoVersions withGradleDebuggingOutput() {
    if (isCi()) {
      def separator = "=".multiply(72)

      println separator
      println "Gradle version:  ${GradleVersion.current().version}"
      println "JVM:             ${System.getProperty('java.version')} (${System.getProperty('java.vm.vendor')} ${System.getProperty('java.vm.version')})"
      println "OS:              ${System.getProperty('os.name')} ${System.getProperty('os.version')} ${System.getProperty('os.arch')}"
      println("")

      println("Tool Versions")
      println(separator)
      println("node: ${toolVersion("node --version")} (corepack: ${toolVersion("corepack --version")})")
      println(" git: ${toolVersion("git --version")}")
      println("  hg: ${toolVersion("hg --quiet --version")}")
      println(" svn: ${toolVersion("svn --quiet --version")}")
      println("  p4: ${toolVersion("p4 -V").readLines().grep(~/Rev.*/).join("")}")
      println(" p4d: ${toolVersion("p4d -V").readLines().grep(~/Rev.*/).join("")}")
      println(" ant: ${toolVersion("ant -version")}")
      println("")

      println("GoCD Versions")
      println(separator)
      println("        goVersion: ${goVersion}")
      println("      distVersion: ${distVersion}        (will be inaccurate on shallow clones!)")
      println("      fullVersion: ${fullVersion}")
      println(" gitShortRevision: ${gitShortRevision}")
      println("targetJavaVersion: ${targetJavaVersion}")
      println(" buildJavaVersion: ${buildJavaVersion}")
      println("   javaPreference: ${preferredJavaVersions()}")
      println("")
    }
    return this
  }

  static boolean isCi() {
    System.getenv().containsKey("GO_SERVER_URL")
  }

  static String getToolVersion(String command, File workingDir = null) {
    try {
      return Optional.ofNullable(runTool(command, workingDir)).map { it.trim() }.orElse("could not determine version!")
    } catch (Exception e) {
      return "could not determine version! (${e.message})"
    }
  }

  static boolean canRunTool(String command, File workingDir = null) {
    try {
      runTool(command, workingDir)
    } catch (Exception ignore) {
      return false
    }
  }

  private static runTool(String command, File workingDir) {
    def process = command.execute(null, workingDir)
    process.waitForOrKill(Duration.ofSeconds(10).toMillis())
    if (process.exitValue() != 0) {
      throw new RuntimeException("'${command}' failed with exit code ${process.exitValue()} and output [${process.in.text}]; error output: [${process.err.text}]")
    }
    return process.text
  }
}
