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
package com.thoughtworks.go.apiv1.buildcause.representers

import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineDependencyModificationRepresenterTest {
  @Test
  void 'should serialize a pipeline dependency modification'() {
    def modification = ModificationsMother.aCheckIn("rev1", "file1")
    def dependencyMaterialRevision = DependencyMaterialRevision.create("pipeline", 50, "1.0.123", "stage", 1)

    def expectedJSON = [
      "_links"       : [
        "vsm"              : [
          "href": "http://test.host/go/pipelines/value_stream_map/pipeline/50"
        ],
        "stage_details_url": [
          "href": "http://test.host/go/pipelines/pipeline/50/stage/1"
        ]
      ],
      "revision"     : "rev1",
      "modified_time": jsonDate(modification.getModifiedTime())
    ]


    def actualJson = toObjectString({
      PipelineDependencyModificationRepresenter.toJSON(it, modification, dependencyMaterialRevision)
    })

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
