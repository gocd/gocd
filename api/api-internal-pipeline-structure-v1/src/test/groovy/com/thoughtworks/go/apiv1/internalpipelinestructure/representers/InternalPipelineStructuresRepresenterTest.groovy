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
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.helper.PipelineTemplateConfigMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class InternalPipelineStructuresRepresenterTest {

  @Test
  void 'should serialize'() {
    def template = PipelineTemplateConfigMother.createTemplate("first-template");
    def template2 = PipelineTemplateConfigMother.createTemplate("second-template");
    def config = PipelineConfigMother.createPipelineConfig("my-pipeline", "my-stage", "my-job1", "my-job2")
    def templateBasedPipeline = PipelineConfigMother.pipelineConfigWithTemplate("my-template-based-pipeline", "first-template");
    def group = PipelineConfigMother.createGroup("first-group", config, templateBasedPipeline)
    def group2 = PipelineConfigMother.createGroup("second-group", config, templateBasedPipeline)

    def json = toObjectString({
      InternalPipelineStructuresRepresenter.toJSON(it, [group, group2], [template, template2])
    })

    assertThatJson(json).isEqualTo([
      groups   : [[
                    name     : 'first-group',
                    pipelines: [
                      [
                        name  : 'my-pipeline',
                        stages: [
                          [
                            name: 'my-stage',
                            jobs: ['my-job1', 'my-job2']
                          ]
                        ]
                      ],
                      [
                        name         : 'my-template-based-pipeline',
                        template_name: "first-template",
                        stages       : []
                      ]
                    ]],
                  [
                    name     : 'second-group',
                    pipelines: [
                      [
                        name  : 'my-pipeline',
                        stages: [
                          [
                            name: 'my-stage',
                            jobs: ['my-job1', 'my-job2']
                          ]
                        ]
                      ],
                      [
                        name         : 'my-template-based-pipeline',
                        template_name: "first-template",
                        stages       : []
                      ]
                    ]]
      ],
      templates: [
        [
          name  : 'first-template',
          stages: [
            [
              name: 'defaultStage',
              jobs: ['defaultJob']
            ]
          ]
        ],
        [
          name  : 'second-template',
          stages: [
            [
              name: 'defaultStage',
              jobs: ['defaultJob']
            ]
          ]
        ]
      ]

    ])
  }
}
