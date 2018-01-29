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

import com.thoughtworks.go.apiv2.dashboard.GoDashboardPipelineMother
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.users.Everyone
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.spark.mocks.TestRequestContext
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.Test

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PipelineGroupsRepresenterTest {

  @Test
  void 'renders pipeline dashboard with hal representation'() {
    def user = new Username(new CaseInsensitiveString(SecureRandom.hex()))
    def permissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE)

    def pipeline_group1 = new GoDashboardPipelineGroup('group1', permissions)
    def pipeline_group2 = new GoDashboardPipelineGroup('group2', permissions)

    def pipeline1_in_group1 = GoDashboardPipelineMother.dashboardPipeline('pipeline1')
    def pipeline2_in_group1 = GoDashboardPipelineMother.dashboardPipeline('pipeline2')
    def pipeline3_in_group2 = GoDashboardPipelineMother.dashboardPipeline('pipeline2')

    pipeline_group1.addPipeline(pipeline1_in_group1)
    pipeline_group1.addPipeline(pipeline2_in_group1)
    pipeline_group2.addPipeline(pipeline3_in_group2)

    def actual_json = PipelineGroupsRepresenter.toJSON([pipeline_group1, pipeline_group2], new TestRequestContext(), user)

    assertThatJson(actual_json._links).isEqualTo([
      self: [href: "http://test.host/go/api/dashboard"],
      doc : [href: "https://api.go.cd/current/#dashboard"]
    ])

    assertThatJson(actual_json._embedded.pipeline_groups).isEqualTo([
      expected_embedded_pipeline_groups(pipeline_group1, user),
      expected_embedded_pipeline_groups(pipeline_group2, user),
    ])

    assertThatJson(actual_json._embedded.pipelines).isEqualTo([
      expected_embedded_pipeline(pipeline1_in_group1, user),
      expected_embedded_pipeline(pipeline2_in_group1, user),
      expected_embedded_pipeline(pipeline3_in_group2, user),
    ])
  }

  private static def expected_embedded_pipeline_groups(model, user) {
    PipelineGroupRepresenter.toJSON(model, new TestRequestContext(), user)
  }

  private static def expected_embedded_pipeline(model, user) {
    PipelineRepresenter.toJSON(model, new TestRequestContext(), user)
  }
}
