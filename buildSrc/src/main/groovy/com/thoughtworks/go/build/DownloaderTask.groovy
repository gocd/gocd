/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import org.gradle.api.tasks.TaskAction
import org.ysb33r.grolifant.api.AbstractDistributionInstaller
import org.ysb33r.grolifant.api.OperatingSystem

class DownloaderTask extends DefaultTask {
  String executable
  String url
  String packageName;
  String packageVersion;

  @TaskAction
  public void perform() {
    AbstractDistributionInstaller installer = createInstaller()
    // perform the download
    File distributionRoot = installer.getDistributionRoot()
    extensions.ext.outputDir = distributionRoot
    extensions.ext.absoluteBinaryPath = project.file("${distributionRoot}/${OperatingSystem.current().getExecutableName(executable)}")
  }

  private AbstractDistributionInstaller createInstaller() {
    new AbstractDistributionInstaller(packageName, packageVersion, "download/${packageName}/${packageVersion}", project) {

      @Override
      URI uriFromVersion(String version) {
        return url.toURI()
      }

      @Override
      protected File getAndVerifyDistributionRoot(File distDir, String distributionDescription) {
        distDir
      }
    }
  }
}
