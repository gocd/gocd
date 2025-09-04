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

import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

class InstallerMetadataTask extends DefaultTask {
  @Input @Optional AdoptiumVersion packagedJavaVersion
  @Input Architecture architecture
  @Internal InstallerType type

  private Provider<FileSystemLocation> distributionArchive

  InstallerMetadataTask() {
    outputs.cacheIf { false }
    outputs.upToDateWhen { false }
  }

  String setPackageTask(TaskProvider<Task> packageTask) {
    super.dependsOn(packageTask)
    this.distributionArchive = packageTask.flatMap {
      it.outputs.getFiles().asFileTree.filter { it.name.startsWith(type.baseName) && !it.name.endsWith(".json") }.elements.map { it.first() }
    } as Provider<FileSystemLocation>
  }

  @TaskAction
  def generateMetadata() {
    getOutputMetadataFile().withWriter { out ->
      out.write(JsonOutput.prettyPrint(JsonOutput.toJson([
        architecture: architecture.canonicalName,
        jre:          packagedJavaVersion?.toMetadata() ?: AdoptiumVersion.noneMetadata(),
      ])))
    }
  }

  @OutputFile
  File getOutputMetadataFile() {
    new File("${distributionArchive.get().asFile.toString()}.json")
  }
}
