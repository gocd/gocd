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
package com.thoughtworks.go.apiv11.shared.representers.stages.tasks

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv11.admin.shared.representers.stages.tasks.TaskRepresenter
import com.thoughtworks.go.config.NantTask
import org.junit.jupiter.api.Test

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class NantTaskRepresenterTest implements TaskRepresenterTrait {
  def existingTask() {
    def task = new NantTask()
    task.setBuildFile("build.xml")
    task.setTarget("package")
    task.setWorkingDirectory("f/m/l")
    return task
  }

  def defaultNewTask() {
    return new NantTask()
  }

  def expectedTaskHash =
    [
      type      : 'nant',
      attributes: [
        working_directory: 'f/m/l',
        build_file       : 'build.xml',
        target           : 'package',
        run_if           : []
      ]
    ]

  def expectedTaskHashWithRunIf =
    [
      type      : 'nant',
      attributes: [
        working_directory: 'f/m/l',
        build_file       : 'build.xml',
        target           : 'package',
        run_if           : ['passed', 'failed', 'any']
      ]
    ]

  def expectedTaskHashWithNoAttributes =
    [
      type      : 'nant'
    ]

  def expectedTaskHashWithOnCancelConfig =
    [
      type      : 'nant',
      attributes: [
        working_directory: 'f/m/l',
        build_file       : 'build.xml',
        target           : 'package',
        run_if           : [],
        on_cancel      : ["type": "ant", attributes:[
          run_if: [],
          working_directory: null,
          build_file: null,
          target: null
        ]]
      ]
    ]

  @Test
  void 'should convert json with no attributes to Task'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(expectedTaskHashWithNoAttributes)
    def task = TaskRepresenter.fromJSON(jsonReader)

    def expectedTask = defaultNewTask()
    assertThatJson(task).isEqualTo(expectedTask)
  }
}
