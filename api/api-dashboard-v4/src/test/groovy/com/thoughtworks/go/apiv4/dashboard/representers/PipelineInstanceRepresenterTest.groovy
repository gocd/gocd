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
package com.thoughtworks.go.apiv4.dashboard.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.TrackingTool
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.permissions.NoOnePermission
import com.thoughtworks.go.config.security.users.NoOne
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.helpers.PipelineModelMother
import com.thoughtworks.go.server.dashboard.Counter
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.helpers.PipelineModelMother.pipeline_model
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PipelineInstanceRepresenterTest {

  @Test
  void 'renders pipeline instance for pipeline that has never been executed with hal representation'() {
    def pipelineInstance = PipelineModelMother.pipeline_instance_model_empty("p1", "s1")
    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(1l)
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
    def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
    pipelineConfig.setOrigin(new FileConfigOrigin())
    pipelineConfig.setTrackingTool(new TrackingTool("http://example.com/\${ID}", "##\\d+"))
    pipelineConfig.setDisplayOrderWeight(0)
    def pipeline = new GoDashboardPipeline(pipeline_model('p1', 'pipeline_label'),
            permissions, "grp", counter, pipelineConfig)
    def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

    def json = toObject({ PipelineInstanceRepresenter.toJSON(it, pipelineInstance, pipeline, username) })

    def expectedJson = [
      _links: [
        self: [href: 'http://test.host/go/api/pipelines/p1/0/instance']
      ],
    ]

    assertThatJson(json._links).isEqualTo(expectedJson._links)
    assertThat(json._embedded.stages[0].name).isEqualTo('s1')
  }

  @Test
  void 'renders all pipeline instance with hal representation'() {
    def instance = PipelineModelMother.pipeline_instance_model([name  : "p1", label: "g1", counter: 5,
                                                                stages: [[name: "cruise", counter: "10", approved_by: "Anonymous"]]])
    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(1l)
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
    def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
    pipelineConfig.setOrigin(new FileConfigOrigin())
    pipelineConfig.setTrackingTool(new TrackingTool("http://example.com/\${ID}", "##\\d+"))
    pipelineConfig.setDisplayOrderWeight(0)
    def pipeline = new GoDashboardPipeline(pipeline_model('p1', 'g1'),
            permissions, "grp", counter, pipelineConfig)
    def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

    def actualJson = toObject({ PipelineInstanceRepresenter.toJSON(it, instance, pipeline, username) })


    actualJson.remove("_links")
    actualJson.remove("_embedded")

    def map = [
      label       : 'g1', counter: 5, scheduled_at: jsonDate(instance.getScheduledDate()),
      triggered_by: 'Triggered by Anonymous'
    ]
    assertThatJson(actualJson).isEqualTo(map)

  }
}
