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

import org.gradle.api.tasks.ParallelizableTask

@ParallelizableTask
class RpmPackagingTask extends LinuxPackagingTask {

  public File getOutputFile() {
    project.file("${project.convention.plugins.get("base").distsDir}/rpm/${packageName}-${version}-${distVersion}.noarch.rpm")
  }

  def packageType() {
    'rpm'
  }

  @Override
  protected List fpmOpts() {
    def cmd = super.fpmOpts()
    cmd += ['-t', packageType()]
    cmd += ['--rpm-defattrfile', '0440']
    cmd += ['--rpm-defattrdir', '0440']

    directories.each {
      dirName, permissions ->
        if (permissions.ownedByPackage) {
          cmd += [
              '--rpm-attr', "${Integer.toOctalString(permissions.mode)},${permissions.owner},${permissions.group}:${dirName}"]
        }
    }

    files.each {
      fileName, permissions ->
        // set ownership and mode on the rpm manifest
        cmd += ['--rpm-attr', "${Integer.toOctalString(permissions.mode)},${permissions.owner},${permissions.group}:${fileName}"]
    }
    cmd
  }

}
