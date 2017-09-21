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
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.internal.os.OperatingSystem

@CacheableTask
class YarnInstallTask extends DefaultTask {

  private File workingDir
  private File nodeModules

  YarnInstallTask() {
    inputs.property('os', OperatingSystem.current().toString())
    project.afterEvaluate({
      inputs.file(project.file("${getWorkingDir()}/package.json"))
      inputs.file(project.file("${getWorkingDir()}/yarn.lock"))
    })

    doLast {
      getNodeModules().setLastModified(System.currentTimeMillis())
    }
  }

  @Input
  // not an @InputFile, because we don't care about the contents of the workingDir itself
  File getWorkingDir() {
    return workingDir
  }

  @OutputDirectory
  File getNodeModules() {
    return nodeModules == null ? project.file("${getWorkingDir()}/node_modules") : nodeModules
  }

  void setWorkingDir(File workingDir) {
    this.workingDir = workingDir
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
      project.delete(getNodeModules())

      project.exec { execTask ->
        execTask.standardOutput = System.out
        execTask.errorOutput = System.err
        execTask.workingDir = this.getWorkingDir()

        execTask.commandLine = [OperatingSystem.current().isWindows() ? "yarn.cmd" : "yarn", "install", "--mutex", "network"]
      }
    }
    setDidWork(shouldExecute)
  }
}
