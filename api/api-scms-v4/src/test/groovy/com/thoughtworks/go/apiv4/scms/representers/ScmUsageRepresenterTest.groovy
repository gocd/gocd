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

package com.thoughtworks.go.apiv4.scms.representers

import com.thoughtworks.go.config.Authorization
import com.thoughtworks.go.config.BasicPipelineConfigs
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigs
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.util.Pair
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static java.util.Collections.emptyList
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ScmUsageRepresenterTest {

  @Test
  void 'should return the map of usages'() {
    def pipelineConfig = PipelineConfigMother.pipelineConfig("some-pipeline")
    Pair<PipelineConfig, PipelineConfigs> pair = new Pair<>(pipelineConfig, new BasicPipelineConfigs("pipeline-group", new Authorization(), pipelineConfig))
    ArrayList<Pair<PipelineConfig, PipelineConfigs>> pairs = new ArrayList<>()
    pairs.add(pair)

    def actualJSON = toObjectString({ ScmUsageRepresenter.toJSON(it, "scm-id", pairs) })
    def expectedJSON = [
      "_links": [
        "self": [
          "href": "http://test.host/go/api/admin/scms/scm-id/usages"
        ],
        "doc" : [
          "href": apiDocsUrl("#scms")
        ],
        "find": [
          "href": "http://test.host/go/api/admin/scms/:material_name/usages"
        ]
      ],
      "usages": [
        [
          "group"   : "pipeline-group",
          "pipeline": "some-pipeline"
        ]
      ]
    ]

    assertThatJson(actualJSON).isEqualTo(expectedJSON)
  }

  @Test
  void 'should return empty list if no usages found'() {
    def actualJSON = toObjectString({ ScmUsageRepresenter.toJSON(it, "scm-id", emptyList()) })
    def expectedJSON = [
      "_links": [
        "self": [
          "href": "http://test.host/go/api/admin/scms/scm-id/usages"
        ],
        "doc" : [
          "href": apiDocsUrl("#scms")
        ],
        "find": [
          "href": "http://test.host/go/api/admin/scms/:material_name/usages"
        ]
      ],
      "usages": []
    ]

    assertThatJson(actualJSON).isEqualTo(expectedJSON)
  }
}
