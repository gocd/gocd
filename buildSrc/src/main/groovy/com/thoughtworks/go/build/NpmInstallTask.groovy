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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.internal.os.OperatingSystem

class NpmInstallTask extends DefaultTask {

  NpmInstallTask() {
    doLast {
      nodeModules().setLastModified(System.currentTimeMillis())
    }
  }

  @InputDirectory
  File workingDir = project.projectDir

  @InputFile
  File packageJSON() {
    return project.file("${workingDir}/package.json")
  }

  @InputFile
  File yarnLock() {
    return project.file("${workingDir}/yarn.lock")
  }

  @InputFile
  File npmShrinkWrap() {
    return project.file("${workingDir}/npm-shrinkwrap.json")
  }

  @OutputDirectory
  File nodeModules() {
    return project.file("${workingDir}/node_modules")
  }

  @Input
  boolean isWindows() {
    OperatingSystem.current().isWindows()
  }

  @TaskAction
  def execute(IncrementalTaskInputs inputs) {
    if (!inputs.incremental) {
      project.delete(nodeModules())
    }

    project.exec { execTask ->
      execTask.standardOutput = System.out
      execTask.errorOutput = System.err
      execTask.workingDir = this.workingDir

      execTask.commandLine = [isWindows() ? "yarn.cmd" : "yarn", "install"]
    }
  }


}
