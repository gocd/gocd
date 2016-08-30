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
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem

public class ExecuteUnderRailsTask extends Exec {

  @Input
  def railsCommand

  public ExecuteUnderRailsTask() {
    dependsOn ':tools:prepareJRuby'
    dependsOn ':server:prepare'
    dependsOn ':server:cleanRails'
    dependsOn ':server:jar'

    if (isWindows()) {
      dependsOn ':server:pathingJar'
    }
  }

  @TaskAction
  public void exec() {
    PrepareRailsCommandHelper helper = new PrepareRailsCommandHelper(project)
    helper.prepare()

    def jrubyOpts = helper.jrubyOpts

    // use pathing jar for windows, else directly pass classpath via `-J-cp`
    if (isWindows()) {
      environment += ['CLASSPATH': project.getTasksByName('pathingJar', false).first().archivePath]
    } else {
      jrubyOpts += '-J-cp'
      jrubyOpts += helper.classpath().asPath
    }

    environment += ['JRUBY_OPTS': jrubyOpts.join(' ')]


    if (OperatingSystem.current().isWindows()) {
      setExecutable project.findProject(':tools').file("rails/bin/jruby.bat")
    } else {
      setExecutable project.findProject(':tools').file("rails/bin/jruby")
    }

    args('-S')

    if (railsCommand instanceof String) {
      args(Commandline.translateCommandline(railsCommand))
    } else {
      args(railsCommand)
    }

    workingDir project.file("webapp/WEB-INF/rails.new")

    debugEnvironment()

    println "[${workingDir}]\$ ${executable} ${args.join(' ')}"

    standardOutput = System.out
    errorOutput = System.err

    super.exec()
  }

  private void debugEnvironment() {
    println "Using environment variables"
    int longestEnv = environment.keySet().sort { a, b -> a.length() - b.length() }.last().length()

    environment.keySet().sort().each { k ->
      println "${k.padLeft(longestEnv)} = ${environment.get(k)}"
    }
  }

  private boolean isWindows() {
    OperatingSystem.current().isWindows()
  }
}
