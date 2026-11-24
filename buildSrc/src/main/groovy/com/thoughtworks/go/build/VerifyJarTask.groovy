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

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip

import javax.inject.Inject

/**
 * Verifies the jars packaged within directories of an archive.
 */
abstract class VerifyJarTask extends AbstractVerifyJarTask {

  @Internal TaskProvider<? extends Zip> jarTask

  @Internal abstract RegularFileProperty getJar()
  @Inject abstract ArchiveOperations getArchiveOps()

  void setJarTask(TaskProvider<? extends Zip> jarTask) {
    this.dependsOn(jarTask)
    this.jarTask = jarTask
    this.jar.set(jarTask.flatMap { it.archiveFile })
  }

  @Override
  protected Collection<String> actualJarNames(String directoryInJar) {
    archiveOps.zipTree(jar).matching {
      include "${directoryInJar}/*.jar"
      include "${directoryInJar}/*.zip"
    }.files.collect { it.name }
  }

  @Override
  protected String describe(String directoryInJar) {
    "${jar.get()}!${directoryInJar}"
  }
}
