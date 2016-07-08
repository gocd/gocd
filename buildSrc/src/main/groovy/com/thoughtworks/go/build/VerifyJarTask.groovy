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

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip

@ParallelizableTask
class VerifyJarTask extends DefaultTask {

  VerifyJarTask() {
    onlyIf { System.getenv().containsKey("GO_SERVER_URL") }
  }

  @Input
  Zip jarTask

  public void setJarTask(Zip jarTask) {
    this.jarTask = jarTask
    this.dependsOn(jarTask)
  }

  @Input
  Map<String, List<String>> expectedJars

  @TaskAction
  public void perform() {
    def expectedMessages = []

    expectedJars.each { directoryInJar, expectedJarsInDir ->
      FileTree tree = project.zipTree(jarTask.archivePath).matching { include "${directoryInJar}/*.jar" }
      def allJars = tree.files.collect { it.name }.sort()

      if (!allJars.equals(expectedJarsInDir.sort())) {
        if (!(allJars - expectedJarsInDir).empty) {
          expectedMessages += ["Got some extra jars in ${jarTask.archivePath}!${directoryInJar} that were not expected"]
          (allJars - expectedJarsInDir).each { jar ->
            expectedMessages += ["  - ${jar}"]
          }
        }

        if (!(expectedJarsInDir - allJars).empty) {
          expectedMessages += ["Some jars that were expected in ${jarTask.archivePath}!${directoryInJar} were not present"]
          (expectedJarsInDir - allJars).each { jar ->
            expectedMessages += ["  - ${jar}"]
          }
        }
      }
    }

    if (!expectedMessages.empty) {
      throw new IllegalStateException(expectedMessages.join(System.getProperty("line.separator")))
    }
  }
}
