/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.ParallelizableTask

@ParallelizableTask
class WindowsPackagingTask extends DefaultTask {
  @Input
  String packageName
  @Input
  String version
  @Input
  String distVersion

  Closure<Task> beforePackage

  @OutputFile
  public File getOutputFile() {
    project.file("${project.convention.plugins.get("base").distsDir}/win/${packageName}-${version}-${distVersion}.exe")
  }

  WindowsPackagingTask() {
    outputs.upToDateWhen { false }
    prepareFileSystem()
    buildPackage()
  }

  def buildRoot() {
    project.file("${project.buildDir}/${packageName}/win/BUILD_ROOT")
  }

  def versionedDir() {
    project.file("${buildRoot()}/${packageName}-${version}")
  }

  def prepareFileSystem() {
    doLast {
      buildRoot().deleteDir()
      buildRoot().mkdirs()
      project.convention.plugins.get("base").distsDir.mkdirs()

      project.exec {
        commandLine "bash", "-c", "set -o pipefail; curl --silent --fail ${jreLocation()} | tar -zxf - -C ${buildRoot()}"
        workingDir buildRoot()

        standardOutput = System.out
        errorOutput = System.err
      }
    }

    doLast {
      beforePackage.call()
    }
  }

  String jreLocation() {
    if (!System.getenv('WINDOWS_JRE_URL')) {
      throw new RuntimeException("Please specify environment variable WINDOWS_JRE_URL")
    }

    System.getenv('WINDOWS_JRE_URL')
  }


  def buildPackage() {
    doLast {
      def names = []
      buildRoot().eachDirMatch(~/jre.*/, { names << it })
      def jreDir = names.first()

      def env = [
          'BINARY_SOURCE_DIR': versionedDir(),
          'LIC_FILE'         : 'LICENSE.dos',
          'NAME'             : packageName.replaceAll(/^go-/, '').capitalize(),
          'MODULE'           : packageName.replaceAll(/^go-/, ''),
          'GO_ICON'          : project.file('windows-shared/gocd.ico').absolutePath,
          'VERSION'          : "${version}-${distVersion}",
          'REGVER'           : "${version}${distVersion.padRight(5, '0')}".replaceAll(/\./, ''),
          'JAVA'             : 'jre',
          'JAVASRC'          : jreDir,
          'DISABLE_LOGGING'  : System.getenv('DISABLE_WIN_INSTALLER_LOGGING') ?: false,
          'OUTDIR'           : "${project.convention.plugins.get("base").distsDir}/win"
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
