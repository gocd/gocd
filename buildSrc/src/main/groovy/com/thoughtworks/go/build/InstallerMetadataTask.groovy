/*
 * Copyright 2024 Thoughtworks, Inc.
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

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.*

class InstallerMetadataTask extends DefaultTask {
  @Input @Optional AdoptiumVersion adoptiumVersion
  @Input Architecture architecture
  @Internal InstallerType type

  private Task packageTask

  InstallerMetadataTask() {
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }
  }

  String setPackageTask(Task packageTask) {
    super.dependsOn(packageTask)
    this.packageTask = packageTask
  }

  @Input
  String getPackageTaskPath() {
    packageTask.path
  }

  @TaskAction
  def generateMetadata() {
    getOutputMetadataFile().withWriter { out ->
      out.write(JsonOutput.prettyPrint(JsonOutput.toJson([
        architecture: architecture.canonicalName,
        jre:          adoptiumVersion?.toMetadata() ?: AdoptiumVersion.noneMetadata(),
      ])))
    }
  }

  @OutputFile
  File getOutputMetadataFile() {
    def distributionArchive = packageTask.outputs.getFiles().asFileTree.filter { it.name.startsWith(type.baseName) && !it.name.endsWith(".json") }.singleFile
    project.file("${ distributionArchive}.json") as File
  }
}
