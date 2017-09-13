/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec

@CacheableTask
class YarnRunTask extends DefaultTask {
  private File workingDir

  private List<String> yarnCommand = new ArrayList<>()
  private List<Object> sourceFiles = new ArrayList<Object>()
  private File destinationDir

  YarnRunTask() {
    inputs.property('os', OperatingSystem.current().toString())

    project.afterEvaluate({
      source(project.file("${getWorkingDir()}/package.json"))
      source(project.file("${getWorkingDir()}/yarn.lock"))
      source(project.file("${getWorkingDir()}/node_modules"))
    })
  }

  @Input
  File getWorkingDir() {
    return workingDir
  }

  @Input
  List<String> getYarnCommand() {
    return yarnCommand
  }

  @OutputDirectory
  File getDestinationDir() {
    return destinationDir
  }

  @SkipWhenEmpty
  @InputFiles
  @PathSensitive(PathSensitivity.NONE)
  FileTree getSourceFiles() {
    ArrayList<Object> copy = new ArrayList<Object>(this.sourceFiles)
    FileTree src = getProject().files(copy).getAsFileTree()
    src == null ? getProject().files().getAsFileTree() : src
  }

  void setYarnCommand(List<String> yarnCommand) {
    this.yarnCommand = new ArrayList<>(yarnCommand)
  }

  void setWorkingDir(Object workingDir) {
    this.workingDir = project.file(workingDir)
  }

  void source(Object... sources) {
    this.sourceFiles.addAll(sources)
  }

  void setDestinationDir(File destinationDir) {
    this.destinationDir = destinationDir
  }

  @TaskAction
  def execute(IncrementalTaskInputs inputs) {
    def shouldExecute = !inputs.incremental

    inputs.outOfDate { change ->
      shouldExecute = true
    }

    inputs.removed { change ->
      shouldExecute = true
    }

    if (shouldExecute) {
      project.delete(getDestinationDir())

      project.exec { ExecSpec execSpec ->
        execSpec.standardOutput = System.out
        execSpec.errorOutput = System.err

        execSpec.workingDir = this.getWorkingDir()
        execSpec.commandLine = [OperatingSystem.current().isWindows() ? "yarn.cmd" : "yarn", "run"] + getYarnCommand()
        println "[${this.getWorkingDir()}]\$ ${execSpec.executable} ${execSpec.args.join(' ')}"
      }
    }

    setDidWork(shouldExecute)
  }
}
