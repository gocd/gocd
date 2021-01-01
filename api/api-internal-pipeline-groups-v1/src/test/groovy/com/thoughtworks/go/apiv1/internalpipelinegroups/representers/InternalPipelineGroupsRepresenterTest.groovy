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
package com.thoughtworks.go.apiv1.internalpipelinegroups.representers

import com.thoughtworks.go.apiv1.internalpipelinegroups.models.PipelineGroupsViewModel
import com.thoughtworks.go.config.BasicEnvironmentConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.EnvironmentsConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.util.Node
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class InternalPipelineGroupsRepresenterTest {

  @Test
  void 'should serialize'() {
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

    def pipelineStructureViewModel = new PipelineGroupsViewModel(new PipelineGroups(group, group2), environmentConfigs)

    def json = toObjectString({
      InternalPipelineGroupsRepresenter.toJSON(it, pipelineStructureViewModel)
    })
    assertThatJson(json).isEqualTo([
      groups: [
        [
          name     : 'first-group',
          pipelines: [
            [
              name       : 'my-pipeline-1',
              origin     : [
                type: "config_repo",
                id  : 'some-config-repo-id'
              ],
              environment: 'test_env'
            ]
          ]
        ],
        [
          name     : 'second-group',
          pipelines: [
            [
              name       : 'my-pipeline-2',
              origin     : [
                type: 'gocd',
              ],
              environment: null
            ],
            [
              name       : 'my-template-based-pipeline',
              environment: null,
            ]
          ]
        ]
      ]
    ])
  }
}
