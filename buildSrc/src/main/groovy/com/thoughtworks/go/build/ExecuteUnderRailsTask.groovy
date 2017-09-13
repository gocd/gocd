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

import org.apache.tools.ant.types.Commandline
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecSpec

class ExecuteUnderRailsTask extends DefaultTask {

  @Input
  def railsCommand

  @Input
  def environment = [:]

  ExecuteUnderRailsTask() {
    dependsOn ':server:cleanRails'
    dependsOn ':server:cleanDb'
    dependsOn ':server:prepareDb'
    dependsOn ':tools:prepareJRuby'
    dependsOn ':server:jar'
    dependsOn ':server:testJar'
    dependsOn ':server:pathingJar'

    def self = this
    doFirst {
      project.exec { ExecSpec execTask ->

        PrepareRailsCommandHelper helper = new PrepareRailsCommandHelper(project)
        helper.prepare()

        def jrubyOpts = helper.jrubyOpts

        execTask.environment += self.environment

        execTask.environment += [
          'CLASSPATH' : project.getTasksByName('pathingJar', false).first().archivePath,
          'JRUBY_OPTS': jrubyOpts.join(' ')
        ]

        if (OperatingSystem.current().isWindows()) {
          setExecutable project.findProject(':tools').file("rails/bin/jruby.bat")
        } else {
          setExecutable project.findProject(':tools').file("rails/bin/jruby")
        }

        args('-S')

        def cmd = railsCommand
        if (cmd instanceof Closure){
          cmd = cmd.call()
        }

        if (cmd instanceof String) {
          args(Commandline.translateCommandline(cmd))
        } else {
          args(cmd)
        }

        workingDir project.file("webapp/WEB-INF/rails.new")

        debugEnvironment(execTask)

        println "[${workingDir}]\$ ${executable} ${args.join(' ')}"

        standardOutput = System.out
        errorOutput = System.err
      }
    }
  }

  static void debugEnvironment(ExecSpec execTask) {
    println "Using environment variables"
    int longestEnv = execTask.environment.keySet().sort { a, b -> a.length() - b.length() }.last().length()

    execTask.environment.keySet().sort().each { k ->
      println "${k.padLeft(longestEnv)} = ${execTask.environment.get(k)}"
    }
  }
}
