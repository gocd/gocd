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
package com.thoughtworks.go.apiv9.shared.representers.stages.tasks

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv9.admin.shared.representers.stages.tasks.TaskRepresenter
import com.thoughtworks.go.config.AntTask
import com.thoughtworks.go.config.RunIfConfig
import com.thoughtworks.go.domain.RunIfConfigs
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

trait TaskRepresenterTrait {

  @Test
  void 'should render task json'() {
    def actualJson = toObjectString({ TaskRepresenter.toJSON(it, existingTask()) })

    assertThatJson(actualJson).isEqualTo(expectedTaskHash)
  }

  @Test
  void 'should render task json with run_if'() {
    def originalTask = existingTask()
    originalTask.setConditions(new RunIfConfigs(RunIfConfig.PASSED, RunIfConfig.FAILED, RunIfConfig.ANY))
    def actualJson = toObjectString({ TaskRepresenter.toJSON(it, originalTask) })

    assertThatJson(actualJson).isEqualTo(expectedTaskHashWithRunIf)
  }

  @Test
  void 'should render task json with oncancel'() {
    def originalTask = existingTask()
    def onCancelTask = new AntTask()
    originalTask.setCancelTask(onCancelTask)
    def actualJson = toObjectString({ TaskRepresenter.toJSON(it, originalTask) })

    assertThatJson(actualJson).isEqualTo(expectedTaskHashWithOnCancelConfig)
  }

  @Test
    void 'should convert json to Task'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(expectedTaskHash)
    def task = TaskRepresenter.fromJSON(jsonReader)

    assertThatJson(task).isEqualTo(existingTask())
  }

  @Test
  void 'should convert json with run if config to Task'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(expectedTaskHashWithRunIf)
    def task = TaskRepresenter.fromJSON(jsonReader)

    def taskWithRunIf = existingTask()
    taskWithRunIf.setConditions(new RunIfConfigs(RunIfConfig.PASSED, RunIfConfig.FAILED, RunIfConfig.ANY))

    assertThatJson(task).isEqualTo(taskWithRunIf)
  }
}
