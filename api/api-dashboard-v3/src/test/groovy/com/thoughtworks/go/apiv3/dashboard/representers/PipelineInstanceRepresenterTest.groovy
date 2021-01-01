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
package com.thoughtworks.go.apiv3.dashboard.representers

import com.thoughtworks.go.helpers.PipelineModelMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class PipelineInstanceRepresenterTest {

  @Test
  void 'renders pipeline instance for pipeline that has never been executed with hal representation'() {
    def pipelineInstance = PipelineModelMother.pipeline_instance_model_empty("p1", "s1")

    def json = toObject({ PipelineInstanceRepresenter.toJSON(it, pipelineInstance) })

    def expectedJson = [
      _links: [
        self       : [href: 'http://test.host/go/api/pipelines/p1/0/instance']
      ],
    ]

    assertThatJson(json._links).isEqualTo(expectedJson._links)
    assertThat(json._embedded.stages[0].name).isEqualTo('s1')
  }

  @Test
  void 'renders all pipeline instance with hal representation'() {
    def instance = PipelineModelMother.pipeline_instance_model([name  : "p1", label: "g1", counter: 5,
                                                                stages: [[name: "cruise", counter: "10", approved_by: "Anonymous"]]])

    def actualJson = toObject({ PipelineInstanceRepresenter.toJSON(it, instance) })


    actualJson.remove("_links")
    actualJson.remove("_embedded")

    def map = [
      label       : 'g1', counter: 5, scheduled_at: jsonDate(instance.getScheduledDate()),
      triggered_by: 'Triggered by Anonymous'
    ]
    assertThatJson(actualJson).isEqualTo(map)

  }
}
