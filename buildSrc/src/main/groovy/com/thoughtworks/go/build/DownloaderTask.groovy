/*
 * Copyright 2022 Thoughtworks, Inc.
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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.ysb33r.grolifant.api.core.OperatingSystem
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.downloader.AbstractDistributionInstaller
import org.ysb33r.grolifant.api.v4.downloader.ArtifactRootVerification
import org.ysb33r.grolifant.api.v4.downloader.ArtifactUnpacker

class DownloaderTask extends DefaultTask {
  @Input
  String executable
  @Input
  String url
  @Input
  String packageName
  @Input
  String packageVersion

  private final ProjectOperations projectOperations

  DownloaderTask() {
    projectOperations = ProjectOperations.create(project)
  }

  @TaskAction
  void perform() {
    AbstractDistributionInstaller installer = createInstaller()
    // perform the download
    File distributionRoot = installer.getDistributionRoot(packageVersion).get()
    extensions.ext.outputDir = distributionRoot
    extensions.ext.absoluteBinaryPath = project.file("${distributionRoot}/${OperatingSystem.current().getExecutableNames(executable).first()}")
  }

  private AbstractDistributionInstaller createInstaller() {
    new GroovyJava16WorkaroundDistributionInstaller(packageName, "download/${packageName}/${packageVersion}", projectOperations) {

      @Override
      URI uriFromVersion(String version) {
        return url.toURI()
      }

      @Override
      protected File verifyDistributionRoot(File distDir) {
        distDir
      }
    }
  }

  /**
   * Workaround for issues with Java 17 and Groovy-generated dynamic proxies used by Grolifant on Groovy 3.0.9
   # https://issues.apache.org/jira/browse/GROOVY-10145 has the fix, but currently seems not backported to 3.x.
   #
   # This is logged at https://gitlab.com/ysb33rOrg/grolifant/-/issues/82 but likely no fix possible unless
   */
  private abstract class GroovyJava16WorkaroundDistributionInstaller extends AbstractDistributionInstaller {
    GroovyJava16WorkaroundDistributionInstaller(String distributionName, String basePath, ProjectOperations projectOperations) {
      super(distributionName, basePath, projectOperations)
      this.artifactRootVerification = new ArtifactRootVerification() {
        @Override
        File apply(File unpackedRoot) {
          return GroovyJava16WorkaroundDistributionInstaller.this.verifyDistributionRoot(unpackedRoot)
        }
      }
      this.artifactUnpacker = new ArtifactUnpacker() {
        @Override
        void accept(File source, File destDir) {
          GroovyJava16WorkaroundDistributionInstaller.this.unpack(source, destDir)
        }
      }
    }
  }
}
