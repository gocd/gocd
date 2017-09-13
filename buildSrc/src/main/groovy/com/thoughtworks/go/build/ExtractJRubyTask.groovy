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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.internal.os.OperatingSystem

@CacheableTask
class ExtractJRubyTask extends DefaultTask {
  @Input
  Configuration gems

  @Input
  Configuration jrubyConfigs

  @OutputDirectory
  File outputDirectory = project.file('jruby')

  @TaskAction
  void doExecute(IncrementalTaskInputs inputs) {
    // locate and download all the binaries
    jrubyConfigs.resolve()
    gems.resolve()

    def shouldExecute = !inputs.incremental

    inputs.outOfDate { change ->
      shouldExecute = true
    }

    inputs.removed { change ->
      shouldExecute = true
    }

    if (shouldExecute) {
      project.delete(outputDirectory)

      extractJRuby()
      installGems()

//      touchTaskOutputs(0)
    }

    setDidWork(shouldExecute)
  }

  private installGems() {
    project.exec {
      standardOutput = System.out
      errorOutput = System.err

      if (OperatingSystem.current().isWindows()) {
        setExecutable project.file("bin/jruby.bat")
      } else {
        setExecutable project.file("bin/jruby")
      }

      args = ['-S', 'gem', 'install', '--quiet', '--no-ri', '--no-rdoc', '--local', '--ignore-dependencies']
      args += gems.files
    }
  }

  private extractJRuby() {
    jrubyConfigs.dependencies.each { dependency ->
      def destinationDir = project.file("jruby-${dependency.version}")

      project.delete(destinationDir)

      project.copy {
        from project.tarTree(jrubyConfigs.singleFile)
        into project.projectDir
      }

      project.file(destinationDir).renameTo(project.file('jruby'))
    }
  }
}
