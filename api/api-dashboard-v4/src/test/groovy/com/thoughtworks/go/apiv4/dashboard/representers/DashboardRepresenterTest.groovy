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

import com.thoughtworks.go.apiv4.dashboard.GoDashboardPipelineMother
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.security.Permissions
import com.thoughtworks.go.config.security.permissions.EveryonePermission
import com.thoughtworks.go.config.security.users.Everyone
import com.thoughtworks.go.server.dashboard.GoDashboardEnvironment
import com.thoughtworks.go.server.dashboard.GoDashboardPipelineGroup
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class DashboardRepresenterTest {

  @Test
  void 'renders pipeline dashboard with hal representation'() {
    def personalizationEtag = "sha256hash"
    def user = new Username(new CaseInsensitiveString(SecureRandom.hex()))
    def permissions = new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, EveryonePermission.INSTANCE)

    def group1 = new GoDashboardPipelineGroup('group1', permissions, true)
    def group2 = new GoDashboardPipelineGroup('group2', permissions, true)

    def pipeline1 = GoDashboardPipelineMother.dashboardPipeline('pipeline1')
    def pipeline2 = GoDashboardPipelineMother.dashboardPipeline('pipeline2')
    def pipeline3 = GoDashboardPipelineMother.dashboardPipeline('pipeline2')

    group1.addPipeline(pipeline1)
    group1.addPipeline(pipeline2)
    group2.addPipeline(pipeline3)

    def env1 = new GoDashboardEnvironment('env1', Everyone.INSTANCE, true)
    env1.addPipeline(pipeline1)
    env1.addPipeline(pipeline3)

    def actualJson = toObject({
      DashboardRepresenter.toJSON(it, new DashboardFor([group1, group2], [env1], user, personalizationEtag))
    })

    assertThatJson(actualJson._links).isEqualTo([
      self: [href: "http://test.host/go/api/dashboard"],
      doc : [href: "https://api.go.cd/current/#dashboard"]
    ])

    assertThatJson(actualJson._embedded.pipeline_groups).isEqualTo([
      toObject({ DashboardGroupRepresenter.toJSON(it, group1, user) }),
      toObject({ DashboardGroupRepresenter.toJSON(it, group2, user) }),
    ])

    assertThatJson(actualJson._embedded.environments).isEqualTo([
      toObject({ DashboardGroupRepresenter.toJSON(it, env1, user) })
    ])

    assertThatJson(actualJson._embedded.pipelines).isEqualTo([
      toObject({ PipelineRepresenter.toJSON(it, pipeline1, user) }),
      toObject({ PipelineRepresenter.toJSON(it, pipeline2, user) }),
      toObject({ PipelineRepresenter.toJSON(it, pipeline3, user) }),
    ])

    assertThat(actualJson._personalization).isEqualTo(personalizationEtag)
  }
}
