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

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.*
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.assertEquals

class PipelineGroupRepresenterTest {

  @Nested
  class Serialize {
    @Test
    void "should represent a pipeline group"() {
      def actualJson = toObjectString({ PipelineGroupRepresenter.toJSON(it, getPipelineConfigs()) })

      assertThatJson(actualJson).isEqualTo(groupHash)
    }
  }

  @Nested
  class Deserialize {

    @Test
    void 'should convert from json to pipeline group'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom(groupHash)
      def pipelineConfigs = PipelineGroupRepresenter.fromJSON(jsonReader)

      assertEquals("group", pipelineConfigs.getGroup())
      assertEquals(getAuthorization(), pipelineConfigs.getAuthorization())
    }

    @Test
    void 'should convert from json without authorization'() {
      def simpleGroup = [
        name: 'groupName'
      ]
      def jsonReader = GsonTransformer.instance.jsonReaderFrom(simpleGroup)
      def pipelineConfigs = PipelineGroupRepresenter.fromJSON(jsonReader)

      assertEquals("groupName", pipelineConfigs.getGroup())
    }
  }

  static def getPipelineConfigs() {
    new BasicPipelineConfigs("group", getAuthorization(), getPipelineConfig())
  }

  static def getAuthorization() {
    new Authorization(new OperationConfig(new AdminUser(new CaseInsensitiveString("user")), new AdminRole(new CaseInsensitiveString("role"))))
  }

  static def getPipelineConfig() {
    def materialConfigs = MaterialConfigsMother.defaultMaterialConfigs()
    new PipelineConfig(new CaseInsensitiveString("pipeline"), materialConfigs)
  }

  def groupHash =
    [
      _links: [
        self: [
          href: 'http://test.host/go/api/admin/pipeline_groups/group'
        ],
        doc: [
          href: 'https://api.go.cd/current/#pipeline-group-config'
        ],
        find: [
          href: 'http://test.host/go/api/admin/pipeline_groups/:group_name'
        ]
      ],
      name: 'group',
      authorization: toObject({ AuthorizationRepresenter.toJSON(it, getAuthorization()) }),
      pipelines: getPipelineConfigs().getPipelines().collect { eachItem ->
        toObject({
          PipelineConfigSummaryRepresenter.toJSON(it, eachItem)
        })
      }
    ]
}
