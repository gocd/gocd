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

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile

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

      File jreDownloadDir = project.file("${winRootDir()}/jre-download")

      jreDownloadDir.deleteDir()
      jreDownloadDir.mkdirs()

      def downloadAction = new DownloadAction(project)
      downloadAction.src(jreLocation())
      downloadAction.dest(jreDownloadDir)
      downloadAction.execute()

      project.copy {
        from project.zipTree(project.fileTree(jreDownloadDir).singleFile)
        into buildRoot()

        // exclude jdk specific stuff
        exclude 'jdk*/lib/src.zip'
        exclude 'jdk*/include/**/*.*'
        exclude 'jdk*/jmods/**/*.*'
      }

      jreDownloadDir.deleteDir()
    }

    doLast {
      beforePackage.call()
    }
  }

  String jreLocation() {
    if (specifiedJreLocation() != null) {
      specifiedJreLocation()
    } else {
      throw new GradleException("Please specify environment variable WINDOWS_${flavour().toUpperCase()}_JDK_URL to point to a windows JDK location.")
    }
  }

  def specifiedJreLocation() {
    System.getenv("WINDOWS_${flavour().toUpperCase()}_JDK_URL")
  }

  def flavour() {
    "64bit"
  }

  def buildPackage() {
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
