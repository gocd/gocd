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
import org.gradle.api.invocation.Gradle

import static org.gradle.api.JavaVersion.*

@Immutable
class GoVersions implements Serializable {
  String goVersion
  String previousVersion
  String nextVersion

  String gitRevision = determineGitRevision()
  String distVersion = determineReleaseRevision()

  String getFullVersion() { distVersion ? "${goVersion}-${distVersion}" : goVersion }

  int targetJavaVersion
  int buildJavaVersion
  AdoptiumVersion packagedJavaVersion

  List<String> preferredJavaVersions() {
    [VERSION_21, VERSION_25, VERSION_29] // LTS versions
      .findAll { v -> v.ordinal() >= toVersion(targetJavaVersion).ordinal() && v.ordinal() <= toVersion(buildJavaVersion).ordinal() }
      .sort()
      .reverse()
      .collect { v -> v.majorVersion }
  }

  private static String determineGitRevision() {
    def process = "git rev-list HEAD --max-count=1".execute(null, null)
    process.waitFor()
    return process.text.stripIndent().trim()
  }

  private static String determineReleaseRevision() {
    def process = "git rev-list HEAD --count".execute(null, null)
    process.waitFor()
    return process.text.stripIndent().trim()
  }

  static void printGradleDebuggingOutputFor(Gradle gradle, GoVersions goVersions) {
    if (System.getenv().containsKey("GO_SERVER_URL")) {
      def separator = "=".multiply(72)

      println separator
      println "Gradle version:  ${gradle.gradleVersion}"
      println "JVM:             ${System.getProperty('java.version')} (${System.getProperty('java.vm.vendor')} ${System.getProperty('java.vm.version')})"
      println "OS:              ${System.getProperty('os.name')} ${System.getProperty('os.version')} ${System.getProperty('os.arch')}"
      println("")

      println("Tool Versions")
      println(separator)
      println("node: ${getToolVersion("node --version")}")
      println(" git: ${getToolVersion("git --version")}")
      println("  hg: ${getToolVersion("hg --quiet --version")}")
      println(" svn: ${getToolVersion("svn --quiet --version")}")
      println("  p4: ${getToolVersion("p4 -V").readLines().grep(~/Rev.*/).join("")}")
      println(" p4d: ${getToolVersion("p4d -V").readLines().grep(~/Rev.*/).join("")}")
      println("")

      println("GoCD Versions")
      println(separator)
      println("        goVersion: ${goVersions.goVersion}")
      println("      distVersion: ${goVersions.distVersion}        (will be inaccurate on shallow clones!)")
      println("      fullVersion: ${goVersions.fullVersion}")
      println("      gitRevision: ${goVersions.gitRevision}")
      println("targetJavaVersion: ${goVersions.targetJavaVersion}")
      println(" buildJavaVersion: ${goVersions.buildJavaVersion}")
      println("   javaPreference: ${goVersions.preferredJavaVersions()}")
      println("")
    }
  }

  private static String getToolVersion(String command) {
    try {
      def process = command.execute(null, null)
      process.waitFor()
      return Optional.ofNullable(process.text).map { it.trim() }.orElse("could not determine version!")
    } catch (Exception e) {
      return "could not determine version! (${e.message})"
    }
  }
}
