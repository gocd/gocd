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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class ExecuteUnderRailsTask extends JRuby {
  @InputFile abstract RegularFileProperty getPathingJar()

  ExecuteUnderRailsTask() {
    dependsOn(':server:initializeRailsGems')
    workingDir = project.railsRoot
  }

  @Override
  @TaskAction
  void exec() {
    classpath(pathingJar.get())
    super.exec()
  }
}
