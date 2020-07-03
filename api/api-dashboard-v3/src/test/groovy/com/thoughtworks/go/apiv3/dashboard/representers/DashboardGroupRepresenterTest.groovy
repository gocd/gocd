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
package com.thoughtworks.go.apiv3.dashboard.representers

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.permissions.EveryonePermission
import com.thoughtworks.go.config.security.users.Everyone
import com.thoughtworks.go.config.security.users.NoOne
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.dashboard.GoDashboardEnvironment
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup
import com.thoughtworks.go.server.dashboard.TimeStampBasedCounter
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.spark.util.SecureRandom
import com.thoughtworks.go.util.Clock
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helpers.PipelineModelMother.pipeline_model
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class DashboardGroupRepresenterTest {

  @Nested
  class PipelineGroups {
    private LinkedHashMap<String, LinkedHashMap<String, String>> expectedLinks = [
      doc : [
        href: 'https://api.go.cd/current/#pipeline-groups'
      ],
      self: [
        href: 'http://test.host/go/api/config/pipeline_groups'
      ]
    ]

    @Test
    void 'renders pipeline group with hal representation'() {
      def permissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE)
      def pipeline1 = dashboardPipeline('pipeline1')
      def pipeline2 = dashboardPipeline('pipeline2')

      def pipelineGroup = new GoDashboardPipelineGroup('group1', permissions, true)
      pipelineGroup.addPipeline(pipeline1)
      pipelineGroup.addPipeline(pipeline2)

      def username = new Username("someone")

      def actualJson = toObjectString({ DashboardGroupRepresenter.toJSON(it, pipelineGroup, username) })

      assertThatJson(actualJson).isEqualTo([
        _links        : expectedLinks,
        name          : 'group1',
        pipelines     : ['pipeline1', 'pipeline2'],
        can_administer: true
      ])
    }

    @Test
    void 'renders pipeline group authorization information'() {
      def noAdminPermissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, NoOne.INSTANCE, EveryonePermission.INSTANCE)

      def pipeline1 = dashboardPipeline('pipeline1')
      def pipeline2 = dashboardPipeline('pipeline2')

      def pipelineGroup = new GoDashboardPipelineGroup('group1', noAdminPermissions, true)
      pipelineGroup.addPipeline(pipeline1)
      pipelineGroup.addPipeline(pipeline2)

      def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

      def actualJson = toObjectString({ DashboardGroupRepresenter.toJSON(it, pipelineGroup, username) })

      assertThatJson(actualJson).isEqualTo([
        _links        : expectedLinks,
        name          : 'group1',
        pipelines     : ['pipeline1', 'pipeline2'],
        can_administer: false
      ])
    }
  }

  @Nested
  class Environments {
    private LinkedHashMap<String, LinkedHashMap<String, String>> expectedLinks = [
      doc : [
        href: apiDocsUrl('#environment-config')
      ],
      self: [
        href: 'http://test.host/go/api/admin/environments/env1'
      ]
    ]

    @Test
    void 'renders pipeline group with hal representation'() {
      def env = new GoDashboardEnvironment('env1', Everyone.INSTANCE, true)
      env.addPipeline(dashboardPipeline('pipeline1'))
      env.addPipeline(dashboardPipeline('pipeline2'))

      def username = new Username("someone")

      def actualJson = toObjectString({ DashboardGroupRepresenter.toJSON(it, env, username) })

      assertThatJson(actualJson).isEqualTo([
        _links        : expectedLinks,
        name          : 'env1',
        pipelines     : ['pipeline1', 'pipeline2'],
        can_administer: true
      ])
    }

    @Test
    void 'renders pipeline group authorization information'() {
      def env = new GoDashboardEnvironment('env1', NoOne.INSTANCE, true)
      env.addPipeline(dashboardPipeline('pipeline1'))
      env.addPipeline(dashboardPipeline('pipeline2'))

      def username = new Username(new CaseInsensitiveString(SecureRandom.hex()))

      def actualJson = toObjectString({ DashboardGroupRepresenter.toJSON(it, env, username) })

      assertThatJson(actualJson).isEqualTo([
        _links        : expectedLinks,
        name          : 'env1',
        pipelines     : ['pipeline1', 'pipeline2'],
        can_administer: false
      ])
    }
  }

  static GoDashboardPipeline dashboardPipeline(pipeline_name, group_name = "group1", permissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE), long timestamp = 1000) {
    def clock = mock(Clock.class)
    when(clock.currentTimeMillis()).thenReturn(timestamp)
    new GoDashboardPipeline(pipeline_model(pipeline_name, 'pipeline-label'), permissions, group_name, new TimeStampBasedCounter(clock), PipelineConfigMother.pipelineConfig(pipeline_name))
  }
}
