/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile

class WindowsPackagingTask extends DefaultTask {
  @Input
  String packageName
  @Input
  String version
  @Input
  String distVersion
  @InputFile
  File openJdkZipFile

  Closure<Task> beforePackage

  @OutputFile
  public File getOutputFile() {
    project.file("${project.convention.plugins.get("base").distsDir}/win/${packageName}-${version}-${distVersion}-jre-${flavour()}-setup.exe")
  }

  WindowsPackagingTask() {
    outputs.upToDateWhen { false }
    prepareFileSystem()
    buildPackage()
  }

  def winRootDir() {
    project.file("${project.buildDir}/${packageName}/win")
  }

  def buildRoot() {
    project.file("${winRootDir()}/BUILD_ROOT")
  }

  def versionedDir() {
    project.file("${buildRoot()}/${packageName}-${version}")
  }

  def prepareFileSystem() {
    doLast {
      buildRoot().deleteDir()
      buildRoot().mkdirs()
      project.convention.plugins.get("base").distsDir.mkdirs()

      project.copy {
        from project.zipTree(openJdkZipFile)
        into buildRoot()

        // exclude jdk specific stuff
        exclude 'jdk*/lib/src.zip'
        exclude 'jdk*/include/**/*.*'
        exclude 'jdk*/jmods/**/*.*'
        includeEmptyDirs = false
      }

    }

    doLast {
      beforePackage.call()
    }
  }

  static def flavour() {
    "64bit"
  }

  private def buildPackage() {
    doLast {
      def names = []
      buildRoot().eachDirMatch(~/jdk.*/, { names << it })
      def jreDir = names.first()

      def env = [
          'BINARY_SOURCE_DIR': versionedDir(),
          'LIC_FILE'         : 'LICENSE.dos',
          'NAME'             : packageName.replaceAll(/^go-/, '').capitalize(),
          'MODULE'           : packageName.replaceAll(/^go-/, ''),
          'GO_ICON'          : project.file('windows-shared/gocd.ico').absolutePath,
          'VERSION'          : "${version}-${distVersion}",
          'REGVER'           : "${version.split(/\./).collect{it.padLeft(2, '0')}.join()}${distVersion.padRight(5, '0')}",
          'JAVA'             : 'jre',
          'JAVASRC'          : jreDir,
          'DISABLE_LOGGING'  : System.getenv('DISABLE_WIN_INSTALLER_LOGGING') ?: false,
          'OUTFILE'           : getOutputFile(),
          'FLAVOUR'           : "${flavour()}"
      ]

      project.exec {
        commandLine "makensis", "-V4", "-NOCD", project.file("${packageName}/win/${packageName}.nsi")
        workingDir buildRoot()
        environment env

        standardOutput = System.out
        errorOutput = System.err
      }
    }
  }
}
