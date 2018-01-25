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
import com.thoughtworks.go.server.dashboard.GoDashboardPipeline
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup
import com.thoughtworks.go.server.dashboard.TimeStampBasedCounter
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.spark.mocks.TestRequestContext
import com.thoughtworks.go.spark.util.SecureRandom
import com.thoughtworks.go.util.Clock
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.apiv2.dashboard.PipelineModelMother.pipeline_model
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class PipelineGroupRepresenterTest {

  private LinkedHashMap<String, LinkedHashMap<String, String>> expectedLinks = [
    doc : [
      href: 'https://api.go.cd/current/#pipeline-groups'
    ],
    self: [
      href: 'http://test.host/api/config/pipeline_groups'
    ]
  ]

  @Test
  void 'renders pipeline group with hal representation'() {
    def permissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE)
    def pipeline1 = dashboardPipeline('pipeline1')
    def pipeline2 = dashboardPipeline('pipeline2')

    def pipelineGroup = new GoDashboardPipelineGroup('group1', permissions)
    pipelineGroup.addPipeline(pipeline1)
    pipelineGroup.addPipeline(pipeline2)

    def actual_json = PipelineGroupRepresenter.toJSON(pipelineGroup, new TestRequestContext(), new Username("someone"))
    JsonFluentAssert.assertThatJson(actual_json).isEqualTo([
      _links        : expectedLinks,
      name          : 'group1',
      pipelines     : ['pipeline1', 'pipeline2'],
      can_administer: true
    ])
  }

  @Test
  void 'renders pipeline group authorization information'() {
    def noAdminPermissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, NoOne.INSTANCE, Everyone.INSTANCE)

    def pipeline1 = dashboardPipeline('pipeline1')
    def pipeline2 = dashboardPipeline('pipeline2')

    def pipelineGroup = new GoDashboardPipelineGroup('group1', noAdminPermissions)
    pipelineGroup.addPipeline(pipeline1)
    pipelineGroup.addPipeline(pipeline2)

    def json = PipelineGroupRepresenter.toJSON(pipelineGroup, new TestRequestContext(),
      new Username(new CaseInsensitiveString(SecureRandom.hex())))

    JsonFluentAssert.assertThatJson(json).isEqualTo([
      _links        : expectedLinks,
      name          : 'group1',
      pipelines     : ['pipeline1', 'pipeline2'],
      can_administer: false
    ])
  }

  static GoDashboardPipeline dashboardPipeline(pipeline_name, group_name = "group1", permissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE), long timestamp = 1000) {
    def clock = mock(Clock.class)
    when(clock.currentTimeMillis()).thenReturn(timestamp)
    new GoDashboardPipeline(pipeline_model(pipeline_name, 'pipeline-label'), permissions, group_name, new TimeStampBasedCounter(clock))
  }

}
