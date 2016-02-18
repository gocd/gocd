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
class DebPackagingTask extends LinuxPackagingTask {

  public File getOutputFile() {
    project.file("${project.convention.plugins.get("base").distsDir}/deb/${packageName}_${version}-${distVersion}_all.deb")
  }

  def packageType() {
    'deb'
  }

  @Override
  protected List fpmOpts() {
    def cmd = super.fpmOpts()
    cmd += ['-t', packageType()]
    cmd += ['--depends', 'java7-runtime-headless']
    cmd += ['--deb-no-default-config-files']

    // HACK: for debian packages :(, since manifests cannot contain fine grained ownership
    def tmpDir = project.file("${project.buildDir}/tmp")
    tmpDir.mkdirs()

    File dirPermissons = File.createTempFile("dirPermissions-", ".json", tmpDir)
    File filePermissions = File.createTempFile("filePermissions-", ".json", tmpDir)

    cmd += ['--template-value', "dir_permissions=${dirPermissons}"]
    cmd += ['--template-value', "file_permissions=${filePermissions}"]

    dirPermissons.write(groovy.json.JsonOutput.toJson(directories))
    filePermissions.write(groovy.json.JsonOutput.toJson(files))

    cmd
  }
}
