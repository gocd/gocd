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
package com.thoughtworks.go.apiv1.internalenvironments.representers

import com.thoughtworks.go.config.EnvironmentAgentConfig
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.helper.MaterialConfigsMother
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class EnvironmentAgentRepresenterTest {
  @Mock
  MergeEnvironmentConfig environmentConfig

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Test
  void 'should represent agent with config xml origin'() {
    def agent = new EnvironmentAgentConfig("agent-1")
    when(environmentConfig.isLocal()).thenReturn(true)
    def actualJSON = toObjectString({ EnvironmentAgentRepresenter.toJSON(it, agent, environmentConfig) })

    def expectedJSON = [
      "uuid"  : agent.uuid,
      "origin": [
        "_links": [
          "self": [
            "href": "http://test.host/go/admin/config_xml"
          ],
          "doc" : [
            "href": apiDocsUrl("#get-configuration")
          ]
        ],
        "type"  : "gocd"
      ]
    ]

    assertThatJson(actualJSON).isEqualTo(expectedJSON)
  }

  @Test
  void 'should represent agent with config repo origin'() {
    def origin = new RepoConfigOrigin(ConfigRepoConfig.createConfigRepoConfig(MaterialConfigsMother.git("foo.git"), "json-plugon", "repo1"), "revision1");
    def agent = new EnvironmentAgentConfig("agent-1")
    when(environmentConfig.isLocal()).thenReturn(false)
    when(environmentConfig.getOriginForAgent(agent.uuid)).thenReturn(origin)
    def actualJSON = toObjectString({ EnvironmentAgentRepresenter.toJSON(it, agent, environmentConfig) })

    def expectedJSON = [
      "uuid"  : agent.uuid,
      "origin": [
        "_links": [
          "doc" : ["href": apiDocsUrl("#config-repos")],
          "find": ["href": "http://test.host/go/api/admin/config_repos/:id"],
          "self": ["href": "http://test.host/go/api/admin/config_repos/repo1"]
        ],
        "id"    : "repo1",
        "type"  : "config_repo"
      ]
    ]

    assertThatJson(actualJSON).isEqualTo(expectedJSON)
  }
}
