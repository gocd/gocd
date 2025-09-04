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


import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.JavaExecSpec

abstract class ExecuteUnderRailsTask extends JavaExec {
  private static final OperatingSystem CURRENT_OS = OperatingSystem.current()
  private Map<String, Object> originalEnv

  @Input
  boolean disableJRubyOptimization = false

  ExecuteUnderRailsTask() {
    super()
    dependsOn(':server:initializeRailsGems', ':server:cleanDb', ':server:createJRubyBinstubs', ':server:pathingJar')

    originalEnv = new LinkedHashMap<String, Object>(environment)
    workingDir = project.railsRoot

    systemProperties += project.railsSystemProperties

    def pathingJarLoc = project.tasks.getByName('pathingJar').archiveFile

    classpath(pathingJarLoc)
    if (CURRENT_OS.isWindows()) {
      environment['CLASSPATH'] += "${File.pathSeparatorChar}${pathingJarLoc.get()}"
    }
    JRuby.setup(this, project, disableJRubyOptimization)
  }


  @Override
  @TaskAction
  void exec() {
    if (CURRENT_OS.isWindows()) {
      environment += [CLASSPATH: project.jrubyJar.get().toString()]
    }

    project.delete(project.rails.testDataDir)

    project.copy {
      from('config')
      into project.rails.testConfigDir
    }

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
