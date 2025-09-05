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

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip

import javax.inject.Inject

abstract class VerifyJarTask extends DefaultTask {

  @Input TaskProvider<? extends Zip> jarTask
  @Input Map<String, List<String>> expectedJars

  @Internal abstract RegularFileProperty getJar()
  @Inject abstract ArchiveOperations getArchiveOps()

  void setJarTask(TaskProvider<? extends Zip> jarTask) {
    this.dependsOn(jarTask)
    this.jarTask = jarTask
    this.jar.set(jarTask.flatMap { it.archiveFile })
  }

  @TaskAction
  void perform() {
    def expectedMessages = []

    expectedJars.each { directoryInJar, expectedJarsInDir ->
      FileTree tree = archiveOps.zipTree(jar).matching {
        include "${directoryInJar}/*.jar"
        include "${directoryInJar}/*.zip"
      }
      def allJars = tree.files.collect { it.name }.sort()

      if (!allJars.equals(expectedJarsInDir.sort())) {
        if (!(allJars - expectedJarsInDir).empty) {
          expectedMessages += ["Got some extra jars in ${jar.get()}!${directoryInJar} that were not expected"]
          (allJars - expectedJarsInDir).each { jar ->
            expectedMessages += ["  - ${jar}"]
          }
        }

        if (!(expectedJarsInDir - allJars).empty) {
          expectedMessages += ["Some jars that were expected in ${jar.get()}!${directoryInJar} were not present"]
          (expectedJarsInDir - allJars).each { jar ->
            expectedMessages += ["  - ${jar}"]
          }
        }
      }
    }

    if (!expectedMessages.empty) {
      throw new IllegalStateException(expectedMessages.join(System.lineSeparator()))
    }
  }
}
