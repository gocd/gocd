/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.dashboard.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.users.Everyone
import com.thoughtworks.go.config.security.users.NoOne
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel
import com.thoughtworks.go.server.dashboard.Counter
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.spark.mocks.TestRequestContext
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.apiv2.dashboard.PipelineModelMother.pipeline_model
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PipelineRepresenterTest {

  @Test
  void 'renders pipeline with hal representation'() {
    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(Long.valueOf(1))
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE);
    def pipeline = new GoDashboardPipeline(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter)
    def json = PipelineRepresenter.toJSON(pipeline, new TestRequestContext(), new Username(new CaseInsensitiveString(SecureRandom.hex())))
    assertThatJson(json).isEqualTo([
      _links                : [
        self                : [href: 'http://test.host/api/pipelines/pipeline_name/history'],
        doc                 : [href: 'https://api.go.cd/current/#pipelines'],
        settings_path       : [href: 'http://test.host/admin/pipelines/pipeline_name/general'],
        trigger             : [href: 'http://test.host/api/pipelines/pipeline_name/schedule'],
        trigger_with_options: [href: 'http://test.host/api/pipelines/pipeline_name/schedule'],
        unpause             : [href: 'http://test.host/api/pipelines/pipeline_name/unpause'],
        pause               : [href: 'http://test.host/api/pipelines/pipeline_name/pause'],
      ],
      _embedded             : [
        instances: [
          expectedEmbeddedPipeline(pipeline.model().activePipelineInstances.first())
        ]
      ],
      name                  : 'pipeline_name',
      locked                : false,
      last_updated_timestamp: 1,
      pause_info            : [
        paused      : false,
        paused_by   : null,
        pause_reason: null
      ],
      can_operate           : false,
      can_administer        : false,
      can_unlock            : false,
      can_pause             : false
    ])
  }

  @Nested
  class Authorization {

    @Test
    void 'user can operate a pipeline if user is pipeline_level operator'() {
      def counter = mock(Counter.class)
      when(counter.getNext()).thenReturn(Long.valueOf(1))
      def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, Everyone.INSTANCE)
      def pipeline = new GoDashboardPipeline(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter)
      def actualJson = PipelineRepresenter.toJSON(pipeline, new TestRequestContext(), new Username(new CaseInsensitiveString(SecureRandom.hex())))

      actualJson.remove("_links")
      actualJson.remove("_embedded")
      def expectedJson = pipelines_hash();
      expectedJson.can_operate = true
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void 'user can administer a pipeline if user is admin of pipeline'() {
      def counter = mock(Counter.class)
      when(counter.getNext()).thenReturn(Long.valueOf(1))
      def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, Everyone.INSTANCE, NoOne.INSTANCE)
      def pipeline = new GoDashboardPipeline(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter)
      def actualJson = PipelineRepresenter.toJSON(pipeline, new TestRequestContext(), new Username(new CaseInsensitiveString(SecureRandom.hex())))
      actualJson.remove("_links")
      actualJson.remove("_embedded")
      def expectedJson = pipelines_hash()
      expectedJson.can_administer = true
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void 'user can unlock and pause a pipeline if user is operator of pipeline'() {
      def counter = mock(Counter.class)
      when(counter.getNext()).thenReturn(Long.valueOf(1))
      def permissions = new Permissions(NoOne.INSTANCE, Everyone.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE)
      def pipeline = new GoDashboardPipeline(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter)
      def actualJson = PipelineRepresenter.toJSON(pipeline, new TestRequestContext(), new Username(new CaseInsensitiveString(SecureRandom.hex())))
      actualJson.remove("_links")
      actualJson.remove("_embedded")
      def expectedJson = pipelines_hash()
      expectedJson.can_unlock = true
      expectedJson.can_pause = true
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }
  }
  private static def expectedEmbeddedPipeline(PipelineInstanceModel pipelineInstanceModel) {
    PipelineInstanceRepresenter.toJSON(pipelineInstanceModel, new TestRequestContext())
  }

  private static def pipelines_hash() {
    return [
      name                  : 'pipeline_name',
      locked                : false,
      last_updated_timestamp: 1,
      pause_info            : [
        paused      : false,
        paused_by   : null,
        pause_reason: null
      ],
      can_operate           : false,
      can_administer        : false,
      can_unlock            : false,
      can_pause             : false
    ]
  }

}
