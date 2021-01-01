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

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.TrackingTool
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.permissions.EveryonePermission
import com.thoughtworks.go.config.security.permissions.NoOnePermission
import com.thoughtworks.go.config.security.users.Everyone
import com.thoughtworks.go.config.security.users.NoOne
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.dashboard.Counter
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.helpers.PipelineModelMother.pipeline_model
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PipelineRepresenterTest {

  @Test
  void 'renders pipeline with hal representation'() {
    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(1l)
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
    def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
    pipelineConfig.setTrackingTool(new TrackingTool("http://example.com/\${ID}", "##\\d+"))
    pipelineConfig.setOrigin(new FileConfigOrigin())
    pipelineConfig.setDisplayOrderWeight(0)
    def pipeline = new GoDashboardPipeline(pipeline_model('pipeline_name', 'pipeline_label'),
            permissions, "grp", counter, pipelineConfig)
    def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

    def json = toObject({ PipelineRepresenter.toJSON(it, pipeline, username) })

    assertThatJson(json).isEqualTo([
      _links                : [
        self    : [href: 'http://test.host/go/api/pipelines/pipeline_name/history'],
        doc     : [href: 'https://api.go.cd/current/#pipelines']
      ],
      _embedded             : [
        instances: [
          toObject({
            PipelineInstanceRepresenter.toJSON(it, pipeline.model().activePipelineInstances.first())
          })
        ]
      ],
      name                  : 'pipeline_name',
      locked                : false,
      last_updated_timestamp: 1,
      pause_info            : [
        paused      : false,
        paused_by   : null,
        pause_reason: null,
        paused_at   : null
      ],
      can_operate           : false,
      can_administer        : false,
      can_unlock            : false,
      can_pause             : false,
      tracking_tool         : [
        "regex": "##\\d+",
        "link" : "http://example.com/\${ID}"
      ],
      from_config_repo      : false
    ])
  }

  @Test
  void 'should consider pipelines with null origin as local'() {
    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(1l)
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
    def pipeline = new GoDashboardPipeline(pipeline_model('p1', 'p1l1'), permissions, "grp", counter, PipelineConfigMother.pipelineConfig("p1"))

    def json = toObject({ PipelineRepresenter.toJSON(it, pipeline, new Username(new CaseInsensitiveString(SecureRandom.hex()))) })

    assertThatJson(json).node("from_config_repo").isEqualTo(false)
  }

  @Test
  void 'should render pause info'() {
    def counter = mock(Counter.class)
    when(counter.getNext()).thenReturn(1l)
    def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
    def pipeline = new GoDashboardPipeline(pipeline_model('p1', 'p1l1', false, true, "under construction"), permissions, "grp", counter, PipelineConfigMother.pipelineConfig("p1"))

    def json = toObject({ PipelineRepresenter.toJSON(it, pipeline, new Username(new CaseInsensitiveString(SecureRandom.hex()))) })

    assertThatJson(json).node("pause_info").isEqualTo([
      paused: true,
      paused_by: 'raghu',
      pause_reason: 'under construction',
      paused_at: '1970-01-01T00:00:12Z'
    ])
  }

  @Nested
  class Authorization {

    @Test
    void 'user can operate a pipeline if user is pipeline_level operator'() {
      def counter = mock(Counter.class)
      when(counter.getNext()).thenReturn(1l)
      def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, NoOne.INSTANCE, EveryonePermission.INSTANCE)
      def origin = new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin", "repo1"), "rev1")
      def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
      pipelineConfig.setOrigin(origin)
      def pipeline = new GoDashboardPipeline(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter, pipelineConfig)
      def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

      def actualJson = toObject({ PipelineRepresenter.toJSON(it, pipeline, username) })

      actualJson.remove("_links")
      actualJson.remove("_embedded")
      def expectedJson = pipelines_hash()
      expectedJson.can_operate = true
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void 'user can administer a pipeline if user is admin of pipeline'() {
      def counter = mock(Counter.class)
      when(counter.getNext()).thenReturn(1l)
      def permissions = new Permissions(NoOne.INSTANCE, NoOne.INSTANCE, Everyone.INSTANCE, NoOnePermission.INSTANCE)
      def origin = new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin", "repo1"), "rev1")
      def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
      pipelineConfig.setOrigin(origin)
      def pipeline = new GoDashboardPipeline(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter, pipelineConfig)
      def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

      def actualJson = toObject({ PipelineRepresenter.toJSON(it, pipeline, username) })

      actualJson.remove("_links")
      actualJson.remove("_embedded")
      def expectedJson = pipelines_hash()
      expectedJson.can_administer = true
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void 'user can unlock and pause a pipeline if user is operator of pipeline'() {
      def counter = mock(Counter.class)
      when(counter.getNext()).thenReturn(1l)
      def permissions = new Permissions(NoOne.INSTANCE, Everyone.INSTANCE, NoOne.INSTANCE, NoOnePermission.INSTANCE)
      def origin = new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig(), "plugin", "repo1"), "rev1")
      def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline_name")
      pipelineConfig.setOrigin(origin)
      def pipeline = new GoDashboardPipeline(pipeline_model('pipeline_name', 'pipeline_label'), permissions, "grp", counter, pipelineConfig)
      def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

      def actualJson = toObject({ PipelineRepresenter.toJSON(it, pipeline, username) })

      actualJson.remove("_links")
      actualJson.remove("_embedded")
      def expectedJson = pipelines_hash()
      expectedJson.can_unlock = true
      expectedJson.can_pause = true
      assertThatJson(actualJson).isEqualTo(expectedJson)
    }
  }

  private static def pipelines_hash() {
    return [
      name                  : 'pipeline_name',
      locked                : false,
      last_updated_timestamp: 1,
      pause_info            : [
        paused      : false,
        paused_by   : null,
        pause_reason: null,
        paused_at   : null
      ],
      can_operate           : false,
      can_administer        : false,
      can_unlock            : false,
      can_pause             : false,
      from_config_repo      : true
    ]
  }

}
