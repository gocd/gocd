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

package com.thoughtworks.go.apiv1.internalpipelinestructure.representers

import com.thoughtworks.go.config.PipelineConfigs
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.spark.Routes
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class InternalPipelineStructuresRepresenterTest {

  @Test
  void 'should serialize'() {
    def group = PipelineConfigMother.createGroup("my-group", PipelineConfigMother.createPipelineConfig("my-pipeline", "my-stage", "my-job1", "my-job2"))

    def json = toArrayString({
      InternalPipelineStructuresRepresenter.toJSON(it, [group])
    })

    assertThatJson(json).isEqualTo([
      [
        name     : 'my-group',
        pipelines: [
          [
            name  : 'my-pipeline',
            stages: [
              [
                name: 'my-stage',
                jobs: ['my-job1', 'my-job2']
              ]
            ]
          ]
        ]
      ]
    ])
  }
}
