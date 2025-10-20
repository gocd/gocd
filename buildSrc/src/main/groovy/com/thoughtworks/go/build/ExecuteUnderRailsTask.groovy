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

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction

abstract class ExecuteUnderRailsTask extends JRuby {
  private Provider<File> bundledGemsPath

  ExecuteUnderRailsTask() {
    def initTask = project.tasks.named('initializeRailsGems')
    dependsOn(initTask)
    workingDir = project.railsRoot
    classpath = classpath.filter { false } // Remove convenience jruby-jar and expect tasks to define their own classpath
    bundledGemsPath = initTask.map { new File(it.outputs.files.first(), 'jruby').listFiles().first() }
  }

  @Override
  @TaskAction
  void exec() {
    // Can't seem to get bundle exec to work under Windows due to some combination of issues related to
    // https://github.com/jruby/jruby/issues/6960. The easiest way seems to be to avoid bundle exec and just use rubygems directly
    // with a bundler-managed GEM_HOME and GEM_PATH, which we do on Linux, Mac and Windows for consistency.
    // The issue seems to be that the Windows `bin` generated are all `@jruby.exe "%~dpn0" %*` and ignores `RUBY` env
    // or any other attempts. Since we do not install jruby executables this does not work.
    environment += [
      GEM_HOME: bundledGemsPath.get(),
      GEM_PATH: bundledGemsPath.get(),
    ]
    additionalPaths += [new File(bundledGemsPath.get(), 'bin')] // Needed for rspec (under Windows with the above)
    super.exec()
  }
}
