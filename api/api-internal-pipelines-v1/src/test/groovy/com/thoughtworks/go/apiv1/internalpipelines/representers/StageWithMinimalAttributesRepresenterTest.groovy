/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.internalpipelines.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.JobConfig
import com.thoughtworks.go.config.JobConfigs
import com.thoughtworks.go.config.StageConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class StageWithMinimalAttributesRepresenterTest {
  @Test
  void 'foo'() {
    def jobConfigs = new JobConfigs(new JobConfig("job1"))
    def stageConfig = new StageConfig(new CaseInsensitiveString("stage1"), jobConfigs)
    def actualJson = toObject({ StageWithMinimalAttributesRepresenter.toJSON(it, stageConfig) })

    def expectedJson = [
      "name": "stage1",
      "jobs": [
        "job1"
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
