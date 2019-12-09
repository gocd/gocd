/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv6.agents.representers

import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.domain.AgentInstance
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.SecurityService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock

import java.util.stream.Stream

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.*
import static com.thoughtworks.go.helper.AgentInstanceMother.*
import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AgentRepresenterTest {
  @Mock
  private SecurityService securityService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Test
  void 'renders an agent with hal representation'() {
    AgentInstance agentInstance = idleWith("some-uuid", "agent01.example.com", "127.0.0.1", "/var/lib/go-server", 10l, "Linux", Arrays.asList("linux", "firefox"))
    agentInstance.getAgent().setEnvironments("uat,load_test,non-existent-env")
    def envFromConfigRepo = environment("dev")
    envFromConfigRepo.setOrigins(new RepoConfigOrigin(new ConfigRepoConfig(null, "yaml", "foo"), "revision"))
    envFromConfigRepo.addAgent("some-uuid")
    def json = toObjectString({
      def environments = Stream.of(environment("uat"), environment("load_test"), envFromConfigRepo)
        .collect()
      AgentRepresenter.toJSON(it, agentInstance, environments, securityService, null)
    })

    def expectedJSON = [
      "_links"            : [
        "self": [
          "href": "http://test.host/go/api/agents/some-uuid"
        ],
        "doc" : [
          "href": apiDocsUrl("#agents")
        ],
        "find": [
          "href": "http://test.host/go/api/agents/:uuid"
        ]
      ],
      "uuid"              : "some-uuid",
      "hostname"          : "agent01.example.com",
      "ip_address"        : "127.0.0.1",
      "sandbox"           : "/var/lib/go-server",
      "operating_system"  : "Linux",
      "free_space"        : 10,
      "agent_config_state": "Enabled",
      "agent_state"       : "Idle",
      "resources"         : ["firefox", "linux"],
      "environments"      : [
        [
          "name"  : "dev",
          "origin": [
            "_links": [
              "doc" : [
                "href": apiDocsUrl("#config-repos")
              ],
              "find": [
                "href": "http://test.host/go/api/admin/config_repos/:id"
              ],
              "self": [
                "href": "http://test.host/go/api/admin/config_repos/foo"
              ]
            ],
            "type"  : "config-repo"
          ]
        ],
        [
          "name"  : "load_test",
          "origin": [
            "_links": [
              "doc" : [
                "href": apiDocsUrl("#get-configuration")
              ],
              "self": [
                "href": "http://test.host/go/admin/config_xml"
              ]
            ],
            "type"  : "gocd"
          ]
        ],
        [
          "name"  : "non-existent-env",
          "origin": [
            "type": "unknown"
          ]
        ],
        [
          "name"  : "uat",
          "origin": [
            "_links": [
              "doc" : [
                "href": apiDocsUrl("#get-configuration")
              ],
              "self": [
                "href": "http://test.host/go/admin/config_xml"
              ]
            ],
            "type"  : "gocd"
          ]
        ]
      ],
      "build_state"       : "Idle"
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  @Test
  void 'renders the elastic agent properties correctly'() {
    AgentInstance agentInstance = idleWith("some-uuid", "agent01.example.com", "127.0.0.1", "/var/lib/go-server", 10l, "Linux", Arrays.asList("linux", "firefox"))
    updateElasticAgentId(agentInstance, "docker-elastic-agent")
    updateElasticPluginId(agentInstance, "cd.go.docker")

    Map<String, Object> map = toObjectWithoutLinks({
      def environments = Stream.of(environment("uat"), environment("load_test")).collect()
      AgentRepresenter.toJSON(it, agentInstance, environments, securityService, null)
    }) as Map<String, Object>

    assertThat(map)
      .containsEntry("elastic_agent_id", "docker-elastic-agent")
      .containsEntry("elastic_plugin_id", "cd.go.docker")
      .doesNotContainKey("resources")
  }

  @Test
  void 'should render environments associated through config-repo'() {
    AgentInstance agentInstance = idleWith("some-uuid", "agent01.example.com", "127.0.0.1", "/var/lib/go-server", 10l, "Linux", Arrays.asList("linux", "firefox"))
    agentInstance.getAgent().setEnvironments("uat")
    def envFromConfigRepo = environment("dev")
    envFromConfigRepo.setOrigins(new RepoConfigOrigin(new ConfigRepoConfig(null, "yaml", "foo"), "revision"))
    envFromConfigRepo.addAgent("some-uuid")
    def json = toObjectString({
      def environments = Stream.of(environment("uat"), envFromConfigRepo)
        .collect()
      AgentRepresenter.toJSON(it, agentInstance, environments, securityService, null)
    })

    def expectedJSON = [
      "_links"            : [
        "self": [
          "href": "http://test.host/go/api/agents/some-uuid"
        ],
        "doc" : [
          "href": apiDocsUrl("#agents")
        ],
        "find": [
          "href": "http://test.host/go/api/agents/:uuid"
        ]
      ],
      "uuid"              : "some-uuid",
      "hostname"          : "agent01.example.com",
      "ip_address"        : "127.0.0.1",
      "sandbox"           : "/var/lib/go-server",
      "operating_system"  : "Linux",
      "free_space"        : 10,
      "agent_config_state": "Enabled",
      "agent_state"       : "Idle",
      "resources"         : ["firefox", "linux"],
      "environments"      : [
        [
          "name"  : "dev",
          "origin": [
            "_links": [
              "doc" : [
                "href": apiDocsUrl("#config-repos")
              ],
              "find": [
                "href": "http://test.host/go/api/admin/config_repos/:id"
              ],
              "self": [
                "href": "http://test.host/go/api/admin/config_repos/foo"
              ]
            ],
            "type"  : "config-repo"
          ]
        ],
        [
          "name"  : "uat",
          "origin": [
            "_links": [
              "doc" : [
                "href": apiDocsUrl("#get-configuration")
              ],
              "self": [
                "href": "http://test.host/go/admin/config_xml"
              ]
            ],
            "type"  : "gocd"
          ]
        ]
      ],
      "build_state"       : "Idle"
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  @Nested
  class BuildDetails {
    private Username username

    @BeforeEach
    void setUp() {
      this.username = new Username("bob")
    }

    @ParameterizedTest
    @MethodSource("agentsBuilding")
    void 'should have build details when agent is building for a user with view or operate permission'(AgentInstance agentInstance) {
      when(securityService.hasViewOrOperatePermissionForPipeline(eq(username), anyString())).thenReturn(true)

      Map<String, Object> map = toObject({
        AgentRepresenter.toJSON(it, agentInstance, Collections.emptyList(), securityService, username)
      }) as Map<String, Object>

      assertThat(map).containsEntry("build_details", [
        "_links"       : [
          "job"     : [
            "href": "http://test.host/go/tab/build/detail/build-windows-PR/3819/build-non-server/1/FastTests-runInstance-4"
          ],
          "stage"   : [
            "href": "http://test.host/go/pipelines/build-windows-PR/3819/build-non-server/1"
          ],
          "pipeline": [
            "href": "http://test.host/go/pipeline/activity/build-windows-PR"
          ]
        ],
        "pipeline_name": "build-windows-PR",
        "stage_name"   : "build-non-server",
        "job_name"     : "FastTests-runInstance-4"
      ])
    }

    @Test
    void 'should not include build details when user does not have view or operate permission for pipeline'() {
      AgentInstance agentInstance = building("build-windows-PR/3819/build-non-server/1/FastTests-runInstance-4")

      when(securityService.hasViewOrOperatePermissionForPipeline(eq(username), anyString())).thenReturn(false)

      Map<String, Object> map = toObject({
        AgentRepresenter.toJSON(it, agentInstance, Collections.emptyList(), securityService, username)
      }) as Map<String, Object>

      assertThat(map).doesNotContainKey("build_details")
    }

    @ParameterizedTest
    @MethodSource("agentsNotBuilding")
    void 'should not include build details when agent is not building'(AgentInstance agentInstance) {
      when(securityService.hasViewOrOperatePermissionForPipeline(eq(username), anyString())).thenReturn(true)

      Map<String, Object> map = toObject({
        AgentRepresenter.toJSON(it, agentInstance, Collections.emptyList(), securityService, username)
      }) as Map<String, Object>

      assertThat(map).doesNotContainKey("build_details")
    }

    @Test
    void 'should represent errors'() {
      AgentInstance agentInstance = agentWithConfigErrors()
      def expectedErrors = [
        ip_address: ["'IP' is an invalid IP address."],
        resources : [
          $/Resource name 'foo%' is not valid. Valid names much match '^[-\w\s|.]*$'/$,
          $/Resource name 'bar$' is not valid. Valid names much match '^[-\w\s|.]*$'/$
        ]
      ]

      Map<String, Object> map = toObjectWithoutLinks({
        AgentRepresenter.toJSON(it, agentInstance, Collections.emptyList(), securityService, username)
      }) as Map<String, Object>

      assertThat(map).containsEntry("errors", expectedErrors)
    }

    private static Stream<Arguments> agentsNotBuilding() {
      return Stream.of(
        Arguments.of(idle()),
        Arguments.of(disabled()),
        Arguments.of(missing())
      )
    }

    private static Stream<Arguments> agentsBuilding() {
      return Stream.of(
        Arguments.of(building("build-windows-PR/3819/build-non-server/1/FastTests-runInstance-4")),
        Arguments.of(cancelled("build-windows-PR/3819/build-non-server/1/FastTests-runInstance-4")),
        Arguments.of(lostContact("build-windows-PR/3819/build-non-server/1/FastTests-runInstance-4"))
      )
    }
  }
}
