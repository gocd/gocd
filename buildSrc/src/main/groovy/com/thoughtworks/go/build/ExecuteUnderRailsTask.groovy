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

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.process.JavaExecSpec

abstract class ExecuteUnderRailsTask extends JavaExec {
  private Map<String, Object> originalEnv

  @InputFiles final FileCollection jrubyJar = project.configurations.jruby as ConfigurableFileCollection
  @InputFile abstract RegularFileProperty getPathingJar()

  ExecuteUnderRailsTask() {
    super()
    dependsOn(':server:initializeRailsGems')

    originalEnv = new LinkedHashMap<String, Object>(environment)
    workingDir = project.railsRoot

    JRuby.setup(this, project)
  }

  @Override
  @TaskAction
  void exec() {
    classpath(pathingJar.get())
    try {
      debugEnvironment(this, originalEnv)
      dumpTaskCommand(this)
      super.exec()
    } finally {
      standardOutput.flush()
      errorOutput.flush()
    }
  }

  static dumpTaskCommand(JavaExecSpec execSpec) {
    println "[${execSpec.workingDir}]\$ java ${execSpec.allJvmArgs.join(' ')} ${execSpec.mainClass.get()} ${execSpec.args.join(' ')}"
  }

  static void debugEnvironment(JavaExecSpec javaExecSpec, Map<String, Object> originalEnv) {
    println "Using environment variables"
    def toDump = javaExecSpec.environment - originalEnv

    int longestEnv = toDump.keySet().sort { a, b -> a.length() - b.length() }.last().length()

    toDump.keySet().sort().each { k ->
      println """${k.padLeft(longestEnv)}='${toDump.get(k)}' \\"""
    }
  }
}
