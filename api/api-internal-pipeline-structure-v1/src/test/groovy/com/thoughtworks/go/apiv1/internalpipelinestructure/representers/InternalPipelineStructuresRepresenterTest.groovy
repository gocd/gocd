/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.apiv1.internalpipelinestructure.models.PipelineStructureViewModel
import com.thoughtworks.go.config.BasicEnvironmentConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.EnvironmentsConfig
import com.thoughtworks.go.config.TemplatesConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.helper.PipelineTemplateConfigMother
import com.thoughtworks.go.util.Node
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Arrays.asList
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class InternalPipelineStructuresRepresenterTest {

  @Test
  void 'should serialize'() {
    def template = PipelineTemplateConfigMother.createTemplateWithParams("first-template", "foo", "bar")
    def template2 = PipelineTemplateConfigMother.createTemplate("second-template")

    def pipeline1 = PipelineConfigMother.createPipelineConfig("my-pipeline-1", "my-stage", "my-job1", "my-job2")
    pipeline1.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(null, null, "some-config-repo-id"), "123"))

    def pipeline2 = PipelineConfigMother.createPipelineConfig("my-pipeline-2", "my-stage", "my-job1", "my-job2")
    pipeline2.setOrigin(new FileConfigOrigin())

    def templateBasedPipeline = PipelineConfigMother.pipelineConfigWithTemplate("my-template-based-pipeline", "first-template")
    def group = PipelineConfigMother.createGroup("first-group", pipeline1)
    def group2 = PipelineConfigMother.createGroup("second-group", pipeline2, templateBasedPipeline)

    def environmentConfigs = new EnvironmentsConfig()
    def environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("test_env"))
    environmentConfig.addPipeline(pipeline1.name)
    environmentConfigs.add(environmentConfig)

    def hashtable = new Hashtable<CaseInsensitiveString, Node>()
    hashtable.put(pipeline1.name, new Node(new Node.DependencyNode(pipeline2.name, pipeline2.stages[0].name())))

    def pipelineStructureViewModel = new PipelineStructureViewModel()
      .setPipelineGroups(new PipelineGroups(group, group2))
      .setTemplatesConfig(new TemplatesConfig(template, template2))
      .setEnvironmentsConfig(environmentConfigs)
      .setPipelineDependencyTable(hashtable)

    def json = toObjectString({
      InternalPipelineStructuresRepresenter.toJSON(it, pipelineStructureViewModel)
    })
    assertThatJson(json).isEqualTo([
      groups   : [[
                    name     : 'first-group',
                    pipelines: [
                      [
                        name               : 'my-pipeline-1',
                        origin             : [
                          type: "config_repo",
                          id  : 'some-config-repo-id'
                        ],
                        environment        : 'test_env',
                        dependant_pipelines: [],
                        stages             : [
                          [
                            name: 'my-stage',
                            jobs: [
                              [name: 'my-job1', is_elastic: false],
                              [name: 'my-job2', is_elastic: false]
                            ]
                          ]
                        ]
                      ]
                    ]],
                  [
                    name     : 'second-group',
                    pipelines: [
                      [
                        name               : 'my-pipeline-2',
                        origin             : [
                          type: 'gocd',
                        ],
                        environment        : null,
                        dependant_pipelines: ['my-pipeline-1'],
                        stages             : [
                          [
                            name: 'my-stage',
                            jobs: [
                              [name: 'my-job1', is_elastic: false],
                              [name: 'my-job2', is_elastic: false]
                            ]
                          ]
                        ]
                      ],
                      [
                        name               : 'my-template-based-pipeline',
                        template_name      : "first-template",
                        environment        : null,
                        dependant_pipelines: [],
                        stages             : [
                          [
                            name: 'defaultStage',
                            jobs: [
                              [name: 'defaultJob', is_elastic: false]
                            ]
                          ]
                        ]
                      ]
                    ]]
      ],
      templates: [
        [
          name      : 'first-template',
          parameters: ['bar', 'foo'],
          stages    : [
            [
              name: 'defaultStage',
              jobs: [
                [name: 'defaultJob', is_elastic: false]
              ]
            ]
          ]
        ],
        [
          name      : 'second-template',
          parameters: [],
          stages    : [
            [
              name: 'defaultStage',
              jobs: [
                [name: 'defaultJob', is_elastic: false]
              ]
            ]
          ]
        ]
      ]

    ])

  }

  @Test
  void 'should serialize with users and roles'() {
    def template = PipelineTemplateConfigMother.createTemplateWithParams("first-template", "foo", "bar")
    def template2 = PipelineTemplateConfigMother.createTemplate("second-template")

    def pipeline1 = PipelineConfigMother.createPipelineConfig("my-pipeline-1", "my-stage", "my-job1", "my-job2")
    pipeline1.setOrigin(new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(null, null, "some-config-repo-id"), "123"))

    def pipeline2 = PipelineConfigMother.createPipelineConfig("my-pipeline-2", "my-stage", "my-job1", "my-job2")
    pipeline2.setOrigin(new FileConfigOrigin())

    def templateBasedPipeline = PipelineConfigMother.pipelineConfigWithTemplate("my-template-based-pipeline", "first-template")
    def group = PipelineConfigMother.createGroup("first-group", pipeline1)
    def group2 = PipelineConfigMother.createGroup("second-group", pipeline2, templateBasedPipeline)
    def users = asList('user1', 'user2')
    def roles = asList('role1', 'role2')

    def pipelineStructureViewModel = new PipelineStructureViewModel()
      .setPipelineGroups(new PipelineGroups(group, group2))
      .setTemplatesConfig(new TemplatesConfig(template, template2))
      .setEnvironmentsConfig(new EnvironmentsConfig())
      .setPipelineDependencyTable(new Hashtable<CaseInsensitiveString, Node>())

    def actualJson = toObjectString({
      InternalPipelineStructuresRepresenter.toJSON(it, pipelineStructureViewModel, users, roles)
    })

    def expectedJson = [
      "groups"         : [
        [
          "name"     : "first-group",
          "pipelines": [
            [
              "name"             : "my-pipeline-1",
              "origin"           : [
                "type": "config_repo",
                "id"  : "some-config-repo-id"
              ],
              environment        : null,
              dependant_pipelines: [],
              "stages"           : [
                [
                  "name": "my-stage",
                  "jobs": [
                    [
                      "name"      : "my-job1",
                      "is_elastic": false
                    ],
                    [
                      "name"      : "my-job2",
                      "is_elastic": false
                    ]
                  ]
                ]
              ]
            ]
          ]
        ],
        [
          "name"     : "second-group",
          "pipelines": [
            [
              "name"             : "my-pipeline-2",
              "origin"           : [
                "type": "gocd"
              ],
              environment        : null,
              dependant_pipelines: [],
              "stages"           : [
                [
                  "name": "my-stage",
                  "jobs": [
                    [
                      "name"      : "my-job1",
                      "is_elastic": false
                    ],
                    [
                      "name"      : "my-job2",
                      "is_elastic": false
                    ]
                  ]
                ]
              ]
            ],
            [
              "name"             : "my-template-based-pipeline",
              "template_name"    : "first-template",
              environment        : null,
              dependant_pipelines: [],
              "stages"           : [
                [
                  "name": "defaultStage",
                  "jobs": [
                    [
                      "name"      : "defaultJob",
                      "is_elastic": false
                    ]
                  ]
                ]
              ]
            ]
          ]
        ]
      ],
      "templates"      : [
        [
          "name"      : "first-template",
          "parameters": [
            "bar",
            "foo"
          ],
          "stages"    : [
            [
              "name": "defaultStage",
              "jobs": [
                [
                  "name"      : "defaultJob",
                  "is_elastic": false
                ]
              ]
            ]
          ]
        ],
        [
          "name"      : "second-template",
          "parameters": [],
          "stages"    : [
            [
              "name": "defaultStage",
              "jobs": [
                [
                  "name"      : "defaultJob",
                  "is_elastic": false
                ]
              ]
            ]
          ]
        ]
      ],
      "additional_info": [
        "users": [
          "user1",
          "user2"
        ],
        "roles": [
          "role1",
          "role2"
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
