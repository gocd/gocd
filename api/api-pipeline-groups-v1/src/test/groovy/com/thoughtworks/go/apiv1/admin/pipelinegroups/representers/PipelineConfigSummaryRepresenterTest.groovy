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
package com.thoughtworks.go.apiv1.admin.pipelinegroups.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineConfigSummaryRepresenterTest {
  @Test
  void "should represent a pipeline"() {
     def actualJson = toObjectString({ PipelineConfigSummaryRepresenter.toJSON(it, getPipelineConfig("name1")) })

     assertThatJson(actualJson).isEqualTo(pipelineHash("name1"))
  }

  @Test
  void "should represent a list of pipelines"() {
    def pipelines = [
            getPipelineConfig("name1"),
            getPipelineConfig("name2"),
            getPipelineConfig("name3")
    ]
    def actualJson = toArrayString({ PipelineConfigSummaryRepresenter.toJSON(it, pipelines)})

    assertThatJson(actualJson).isEqualTo([pipelineHash("name1"), pipelineHash("name2"), pipelineHash("name3")])
  }

  static def getPipelineConfig(String name) {
      def materialConfigs = MaterialConfigsMother.defaultMaterialConfigs()
      new PipelineConfig(new CaseInsensitiveString(name), materialConfigs)
  }

  static def pipelineHash(String name) {
    [
      _links : [
        self: [
          href: "http://test.host/go/api/admin/pipelines/$name".toString()
        ],
        doc: [
          href: apiDocsUrl("#pipeline-config")
        ],
        find: [
          href: "http://test.host/go/api/admin/pipelines/:pipeline_name"
        ]
      ],
      name : name
    ]
  }
}
