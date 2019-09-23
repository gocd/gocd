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

import com.thoughtworks.go.helper.PipelineConfigMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineConfigsWithMinimalAttributesRepresenterTest {
  @Test
  void 'should return pipeline config json with minimal attributes'() {
    def groups = PipelineConfigMother.createGroups("group1", "group2")
    def actualJson = toObject({ PipelineConfigsWithMinimalAttributesRepresenter.toJSON(it, groups) })

    def expectedJson = [
      "_links"   : [
        "self": [
          "href": "http://test.host/go/api/admin/internal/pipelines"
        ]
      ],
      "_embedded": [
        "pipelines": [
          [
            "_links": [
              "self": [
                "href": "http://test.host/go/api/admin/pipelines/pipeline_0"
              ]
            ],
            "name"  : "pipeline_0",
            "stages": [
              [
                "name": "mingle",
                "jobs": []
              ]
            ]
          ],
          [
            "_links": [
              "self": [
                "href": "http://test.host/go/api/admin/pipelines/pipeline_1"
              ]
            ],
            "name"  : "pipeline_1",
            "stages": [
              [
                "name": "mingle",
                "jobs": []
              ]
            ]
          ],
          [
            "_links": [
              "self": [
                "href": "http://test.host/go/api/admin/pipelines/pipeline_2"
              ]
            ],
            "name"  : "pipeline_2",
            "stages": [
              [
                "name": "mingle",
                "jobs": []
              ]
            ]
          ],
          [
            "_links": [
              "self": [
                "href": "http://test.host/go/api/admin/pipelines/pipeline_3"
              ]
            ],
            "name"  : "pipeline_3",
            "stages": [
              [
                "name": "mingle",
                "jobs": []
              ]
            ]
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}