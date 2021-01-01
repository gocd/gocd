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
package com.thoughtworks.go.apiv1.admin.pipelinegroups.representers

import com.thoughtworks.go.config.*
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineGroupsRepresenterTest {
  @Test
  void "should represent an empty collection of groups"() {
    def actualJson = toObjectString({ PipelineGroupsRepresenter.toJSON(it, new PipelineGroups()) })

    assertThatJson(actualJson).isEqualTo(groupHash([]))
  }

  @Test
  void "should represent a collection of groups"() {
    def groups = [
      getPipelineConfigs("group1", "pipeline1"),
      getPipelineConfigs("group2", "pipeline2")
    ]
    def actualJson = toObjectString({ PipelineGroupsRepresenter.toJSON(it, new PipelineGroups(*groups)) })

    assertThatJson(actualJson).isEqualTo(groupHash(groups))
  }

  static def getPipelineConfigs(String groupName, String pipelineName) {
    new BasicPipelineConfigs(groupName, getAuthorization(), getPipelineConfig(pipelineName))
  }

  static def getAuthorization() {
    new Authorization(new OperationConfig(new AdminUser(new CaseInsensitiveString("user")), new AdminRole(new CaseInsensitiveString("role"))))
  }

  static def getPipelineConfig(String pipelineName) {
    def materialConfigs = MaterialConfigsMother.defaultMaterialConfigs()
    new PipelineConfig(new CaseInsensitiveString(pipelineName), materialConfigs)
  }

  static def groupHash(List<PipelineConfigs> groups) {
    [
      _links   : [
        self: [
          href: 'http://test.host/go/api/admin/pipeline_groups'
        ],
        doc: [
          href: 'https://api.go.cd/current/#pipeline-group-config'
        ],
        find: [
          href: 'http://test.host/go/api/admin/pipeline_groups/:group_name'
        ]
      ],
      _embedded: [
        'groups': groups.collect { eachItem -> toObject({ PipelineGroupRepresenter.toJSON(it, eachItem) }) }
      ]
    ]
  }
}
