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

import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar
import org.gradle.process.JavaExecSpec

import javax.inject.Inject

abstract class ExecuteUnderRailsTask extends JavaExec {
  private static final OperatingSystem CURRENT_OS = OperatingSystem.current()
  private Map<String, Object> originalEnv

  @Input boolean disableJRubyOptimization = false
  @Internal final projectRailsMeta = project.rails
  @Internal final Provider<File> jrubyJar = project.jrubyJar
  @Inject abstract FileSystemOperations getFileOps()

  ExecuteUnderRailsTask() {
    super()
    dependsOn(':server:initializeRailsGems', ':server:cleanDb', ':server:createJRubyBinstubs', ':server:pathingJar')

    originalEnv = new LinkedHashMap<String, Object>(environment)
    workingDir = project.railsRoot

    systemProperties += project.railsSystemProperties

    def pathingJarLoc = project.tasks.named('pathingJar').flatMap { Jar jt -> jt.archiveFile } as Provider<RegularFile>

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
      environment += [CLASSPATH: jrubyJar.get().toString()]
    }

    fileOps.delete {
      it.delete(this.projectRailsMeta.testDataDir)
    }

    fileOps.copy {
      it.from('config')
      it.into(this.projectRailsMeta.testConfigDir)
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
