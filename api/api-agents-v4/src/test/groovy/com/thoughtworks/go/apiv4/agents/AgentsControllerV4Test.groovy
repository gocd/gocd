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
package com.thoughtworks.go.apiv4.agents

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.EnvironmentsConfig
import com.thoughtworks.go.domain.AgentInstance
import com.thoughtworks.go.domain.NullAgentInstance
import com.thoughtworks.go.server.service.AgentService
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.result.HttpOperationResult
import com.thoughtworks.go.server.service.result.LocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.TriState
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import java.util.stream.Stream

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.helper.AgentInstanceMother.*
import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment
import static com.thoughtworks.go.serverhealth.HealthStateScope.GLOBAL
import static com.thoughtworks.go.serverhealth.HealthStateType.general
import static java.lang.String.format
import static java.util.Arrays.asList
import static java.util.Collections.*
import static java.util.stream.Collectors.toSet
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class AgentsControllerV4Test implements SecurityServiceTrait, ControllerTrait<AgentsControllerV4> {
  @Mock
  private AgentService agentService

  @Mock
  private EnvironmentConfigService environmentConfigService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  AgentsControllerV4 createControllerInstance() {
    return new AgentsControllerV4(agentService, new ApiAuthenticationHelper(securityService, goConfigService), securityService, environmentConfigService)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'index'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Test
    void "should return a list of agents"() {
      when(agentService.getAgentInstanceToSortedEnvMap()).thenReturn(new HashMap<AgentInstance, Collection<String>>() {
        {
          put(idle(), asList("env1", "env2"))
        }
      })

      getWithApiHeader(controller.controllerPath())

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasJsonBody([
        "_links"   : [
          "self": [
            "href": "http://test.host/go/api/agents"
          ],
          "doc" : [
            "href": apiDocsUrl("#agents")
          ]
        ],
        "_embedded": [
          "agents": [
            [
              "_links"            : [
                "self": [
                  "href": "http://test.host/go/api/agents/uuid2"
                ],
                "doc" : [
                  "href": apiDocsUrl("#agents")
                ],
                "find": [
                  "href": "http://test.host/go/api/agents/:uuid"
                ]
              ],
              "uuid"              : "uuid2",
              "hostname"          : "CCeDev01",
              "ip_address"        : "10.18.5.1",
              "sandbox"           : "/var/lib/foo",
              "operating_system"  : "",
              "free_space"        : 10240,
              "agent_config_state": "Enabled",
              "agent_state"       : "Idle",
              "resources"         : [],
              "environments"      : [
                "env1",
                "env2"
              ],
              "build_state"       : "Idle"
            ]
          ]
        ]
      ])
    }
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, NormalUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'show'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("/some-uuid"))
      }
    }

    @Test
    void 'should return agent json'() {
      when(agentService.findAgent("uuid2")).thenReturn(idle())
      when(environmentConfigService.environmentsFor("uuid2")).thenReturn(Stream.of("env1", "env2").collect(toSet()))

      getWithApiHeader(controller.controllerPath("/uuid2"))

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasJsonBody([
        "_links"            : [
          "self": [
            "href": "http://test.host/go/api/agents/uuid2"
          ],
          "doc" : [
            "href": apiDocsUrl("#agents")
          ],
          "find": [
            "href": "http://test.host/go/api/agents/:uuid"
          ]
        ],
        "uuid"              : "uuid2",
        "hostname"          : "CCeDev01",
        "ip_address"        : "10.18.5.1",
        "sandbox"           : "/var/lib/foo",
        "operating_system"  : "",
        "free_space"        : 10240,
        "agent_config_state": "Enabled",
        "agent_state"       : "Idle",
        "resources"         : [],
        "environments"      : [
          "env1",
          "env2"
        ],
        "build_state"       : "Idle"
      ])
    }

    @Test
    void 'should return 404 when agent with uuid does not exist'() {
      when(agentService.findAgent("uuid2")).thenReturn(new NullAgentInstance())

      getWithApiHeader(controller.controllerPath("/uuid2"))

      assertThatResponse()
        .isNotFound()
        .hasJsonMessage(controller.entityType.notFoundMessage("uuid2"))
        .hasContentType(controller.mimeType)
    }
  }

  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'update'
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath("/some-uuid"), [])
      }
    }

    @Test
    void 'should update agent information'() {
      loginAsAdmin()
      AgentInstance updatedAgentInstance = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", asList("psql", "java"))
      def environmentsConfig = new EnvironmentsConfig()
      def environmentConfig = environment("env1")

      when(environmentConfigService.findOrDefault("env1")).thenReturn(environmentConfig)
      when(environmentConfigService.environmentsFor("uuid2")).thenReturn(singleton("env1"))

      environmentsConfig.add(environmentConfig)
      when(agentService.updateAgentAttributes(
        eq("uuid2"),
        eq("agent02.example.com"),
        eq("java,psql"),
        eq(environmentsConfig),
        eq(TriState.TRUE),
        any() as HttpOperationResult)
      ).thenReturn(updatedAgentInstance)

      def requestBody = ["hostname"          : "agent02.example.com",
                         "agent_config_state": "Enabled",
                         "resources"         : ["java", "psql"],
                         "environments"      : ["env1"]
      ]
      patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasJsonBody([
        "_links"            : [
          "self": [
            "href": "http://test.host/go/api/agents/uuid2"
          ],
          "doc" : [
            "href": apiDocsUrl("#agents")
          ],
          "find": [
            "href": "http://test.host/go/api/agents/:uuid"
          ]
        ],
        "uuid"              : "uuid2",
        "hostname"          : "agent02.example.com",
        "ip_address"        : "10.0.0.1",
        "sandbox"           : "/var/lib/bar",
        "operating_system"  : "",
        "free_space"        : 10,
        "agent_config_state": "Enabled",
        "agent_state"       : "Idle",
        "resources"         : ["java", "psql"],
        "environments"      : ["env1"],
        "build_state"       : "Idle"
      ])
    }

    @Test
    void 'should error out when operation is unsuccessful'() {
      loginAsAdmin()
      when(agentService.updateAgentAttributes(anyString(), anyString(), anyString(), anyList() as EnvironmentsConfig,
        any() as TriState, any() as HttpOperationResult)).thenAnswer({ InvocationOnMock invocation ->
        def result = invocation.getArgument(5) as HttpOperationResult
        result.unprocessibleEntity("Not a valid operation", "some description", null)
        return idle()
      })

      def requestBody = [
        "hostname"          : "agent02.example.com",
        "agent_config_state": "",
        "resources"         : "Java,Linux",
        "environments"      : ["Foo"]
      ]

      patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)

      assertThatResponse()
        .isUnprocessableEntity()
        .hasContentType(controller.mimeType)
        .hasJsonBody([
        "message": "Not a valid operation",
        "data"   : [
          "uuid"              : "uuid2",
          "hostname"          : "CCeDev01",
          "ip_address"        : "10.18.5.1",
          "sandbox"           : "/var/lib/foo",
          "operating_system"  : "",
          "free_space"        : 10240,
          "agent_config_state": "Enabled",
          "agent_state"       : "Idle",
          "resources"         : [],
          "environments"      : [],
          "build_state"       : "Idle"
        ]
      ])
    }

    @Nested
    class Environments {
      @Test
      void 'should pass proper environments config object to service given comma separated list of environments'() {
        loginAsAdmin()
        AgentInstance updatedAgentInstance = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", asList("psql", "java"))

        def environmentsConfig = new EnvironmentsConfig()
        def environmentConfig = environment("env1")
        def environmentConfig1 = environment("env2")
        environmentsConfig.add(environmentConfig)
        environmentsConfig.add(environmentConfig1)

        def commaSeparatedEnvs = "   env1, env2 "

        when(environmentConfigService.findOrDefault("env1")).thenReturn(environmentConfig)
        when(environmentConfigService.findOrDefault("env2")).thenReturn(environmentConfig1)

        when(environmentConfigService.environmentsFor("uuid2")).thenReturn(singleton("env1"))

        when(agentService.updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(environmentsConfig),
          eq(TriState.TRUE),
          any() as HttpOperationResult)
        ).thenReturn(updatedAgentInstance)

        def requestBody = ["hostname"          : "agent02.example.com",
                           "agent_config_state": "Enabled",
                           "resources"         : ["java", "psql"],
                           "environments"      : commaSeparatedEnvs
        ]
        patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)

        verify(agentService).updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(environmentsConfig),
          eq(TriState.TRUE),
          any() as HttpOperationResult)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody([
          "_links"            : [
            "self": [
              "href": "http://test.host/go/api/agents/uuid2"
            ],
            "doc" : [
              "href": apiDocsUrl("#agents")
            ],
            "find": [
              "href": "http://test.host/go/api/agents/:uuid"
            ]
          ],
          "uuid"              : "uuid2",
          "hostname"          : "agent02.example.com",
          "ip_address"        : "10.0.0.1",
          "sandbox"           : "/var/lib/bar",
          "operating_system"  : "",
          "free_space"        : 10,
          "agent_config_state": "Enabled",
          "agent_state"       : "Idle",
          "resources"         : ["java", "psql"],
          "environments"      : ["env1"],
          "build_state"       : "Idle"
        ])
      }

      @Test
      void 'should pass empty environments config object to service given empty comma separated list of environments'() {
        loginAsAdmin()
        AgentInstance updatedAgentInstance = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", asList("psql", "java"))

        def environmentsConfig = new EnvironmentsConfig()

        def commaSeparatedEnvs = "             "

        when(agentService.updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(environmentsConfig),
          eq(TriState.TRUE),
          any() as HttpOperationResult)
        ).thenReturn(updatedAgentInstance)

        def requestBody = ["hostname"          : "agent02.example.com",
                           "agent_config_state": "Enabled",
                           "resources"         : ["java", "psql"],
                           "environments"      : commaSeparatedEnvs
        ]
        patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)

        verify(agentService).updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(environmentsConfig),
          eq(TriState.TRUE),
          any() as HttpOperationResult
        )

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody([
          "_links"            : [
            "self": [
              "href": "http://test.host/go/api/agents/uuid2"
            ],
            "doc" : [
              "href": apiDocsUrl("#agents")
            ],
            "find": [
              "href": "http://test.host/go/api/agents/:uuid"
            ]
          ],
          "uuid"              : "uuid2",
          "hostname"          : "agent02.example.com",
          "ip_address"        : "10.0.0.1",
          "sandbox"           : "/var/lib/bar",
          "operating_system"  : "",
          "free_space"        : 10,
          "agent_config_state": "Enabled",
          "agent_state"       : "Idle",
          "resources"         : ["java", "psql"],
          "environments"      : [],
          "build_state"       : "Idle"
        ])
      }

      @Test
      void 'should pass null as environments config object to service given null comma separated list of environments'() {
        loginAsAdmin()
        AgentInstance updatedAgentInstance = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", asList("psql", "java"))

        def environmentsConfig = new EnvironmentsConfig()

        when(agentService.updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(null),
          eq(TriState.TRUE),
          any() as HttpOperationResult)
        ).thenReturn(updatedAgentInstance)

        def requestBody = ["hostname"          : "agent02.example.com",
                           "agent_config_state": "Enabled",
                           "resources"         : ["java", "psql"]
        ]
        patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)

        verify(agentService).updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(null),
          eq(TriState.TRUE),
          any() as HttpOperationResult
        )

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody([
          "_links"            : [
            "self": [
              "href": "http://test.host/go/api/agents/uuid2"
            ],
            "doc" : [
              "href": apiDocsUrl("#agents")
            ],
            "find": [
              "href": "http://test.host/go/api/agents/:uuid"
            ]
          ],
          "uuid"              : "uuid2",
          "hostname"          : "agent02.example.com",
          "ip_address"        : "10.0.0.1",
          "sandbox"           : "/var/lib/bar",
          "operating_system"  : "",
          "free_space"        : 10,
          "agent_config_state": "Enabled",
          "agent_state"       : "Idle",
          "resources"         : ["java", "psql"],
          "environments"      : [],
          "build_state"       : "Idle"
        ])
      }
    }
  }

  @Nested
  class BulkUpdate {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'bulkUpdate'
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath(), [])
      }
    }

    @Test
    void 'should update agents information for specified agents'() {
      loginAsAdmin()
      doAnswer({ InvocationOnMock invocation ->
        def result = invocation.getArgument(6) as HttpLocalizedOperationResult
        result.setMessage("Updated agent(s) with uuid(s): [agent-1, agent-2].")

      }).when(agentService).bulkUpdateAgentAttributes(
        any() as List<String>,
        any() as List<String>,
        any() as List<String>,
        any() as EnvironmentsConfig,
        any() as List<String>,
        any() as TriState,
        any() as LocalizedOperationResult
      )

      def requestBody = [
        "uuids"             : [
          "adb9540a-b954-4571-9d9b-2f330739d4da",
          "adb528b2-b954-1234-9d9b-b27ag4h568e1"
        ],
        "operations"        : [
          "environments": [
            "add"   : ["Dev", "Test"],
            "remove": ["Production"]
          ],
          "resources"   : [
            "add"   : ["Linux", "Firefox"],
            "remove": ["Chrome"]
          ]
        ],
        "agent_config_state": "enabled"
      ]

      patchWithApiHeader(controller.controllerPath(), requestBody)

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Updated agent(s) with uuid(s): [agent-1, agent-2].")
    }
  }

  @Nested
  class Delete {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'deleteAgent'
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath("some-uuid"))
      }
    }

    @Nested
    class PositiveP {
      @Test
      void 'should delete agent with given uuid'() {
        loginAsAdmin()

        when(agentService.findAgent("uuid2")).thenReturn(idle())
        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.ok("Deleted 1 agent(s).")
        }).when(agentService).deleteAgents(eq(asList("uuid2")), any() as HttpOperationResult)

        deleteWithApiHeader(controller.controllerPath("uuid2"))

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Deleted 1 agent(s).")
      }
    }

    @Nested
    class Negative {
      @Test
      void 'delete agent should throw 404 when called with UUID that does not exist'() {
        loginAsAdmin()

        def nonExistingUUID = "non-existing-uuid"

        when(agentService.findAgent(nonExistingUUID)).thenReturn(new NullAgentInstance(nonExistingUUID))

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.notFound("Not Found", format("Agent '%s' not found", nonExistingUUID), general(GLOBAL))
        }).when(agentService).deleteAgents(eq(singletonList(nonExistingUUID)), any() as HttpOperationResult)

        deleteWithApiHeader(controller.controllerPath(nonExistingUUID))

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Not Found { Agent 'non-existing-uuid' not found }")
      }

      @Test
      void 'should render result in case of error'() {
        loginAsAdmin()

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          def message = "Failed to delete agent."
          result.unprocessibleEntity(message, "Some description", null)
        }).when(agentService).deleteAgents(eq(asList("uuid2")), any() as HttpOperationResult)

        deleteWithApiHeader(controller.controllerPath("uuid2"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Failed to delete agent. { Some description }")
      }
    }
  }

  @Nested
  class BulkDelete {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'bulkDeleteAgents'
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class Positive {
      @Test
      void 'should delete agents with uuids'() {
        loginAsAdmin()

        when(agentService.findAgent("agent-1")).thenReturn(idleWith("agent-1"))
        when(agentService.findAgent("agent-2")).thenReturn(idleWith("agent-2"))

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.ok("Deleted 2 agent(s).")
        }).when(agentService).deleteAgents(eq(asList("agent-1", "agent-2")), any() as HttpOperationResult)

        def requestBody = ["uuids": ["agent-1", "agent-2"]]

        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Deleted 2 agent(s).")
      }

      @Test
      void 'should delete agents with uuids when all specified UUIDs are disabled'() {
        loginAsAdmin()

        def disabledUUID1 = "uuid1"
        def disabledUUID2 = "uuid2"

        def disabledAgent1 = disabledWith(disabledUUID1)
        def disabledAgent2 = disabledWith(disabledUUID2)

        when(agentService.findAgent(disabledUUID1)).thenReturn(disabledAgent1)
        when(agentService.findAgent(disabledUUID2)).thenReturn(disabledAgent2)

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.ok("Deleted 2 agent(s).")
        }).when(agentService).deleteAgents(eq(asList(disabledUUID1, disabledUUID2)), any() as HttpOperationResult)

        def requestBody = ["uuids": [disabledUUID1, disabledUUID2]]

        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Deleted 2 agent(s).")
      }

      @Test
      void 'should delete agents with uuids when list of UUIDs is passed as null'() {
        loginAsAdmin()

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.ok("Deleted 0 agent(s).")
        }).when(agentService).deleteAgents(eq(emptyList()), any() as HttpOperationResult)

        def requestBody = null

        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Deleted 0 agent(s).")
      }

      @Test
      void 'should delete agents with uuids when empty list of UUIDs is passed'() {
        loginAsAdmin()

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.ok("Deleted 0 agent(s).")
        }).when(agentService).deleteAgents(eq(emptyList()), any() as HttpOperationResult)

        def requestBody = [ "uuids":[] ]

        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Deleted 0 agent(s).")
      }
    }

    @Nested
    class Negative {
      @Test
      void 'delete agents should throw 404 when called with list of UUIDs that do not exist'() {
        loginAsAdmin()

        def nonExistingUUID = "non-existing-uuid"

        when(agentService.findAgent(nonExistingUUID)).thenReturn(new NullAgentInstance(nonExistingUUID))

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.notFound("Not Found", format("Agent '%s' not found", nonExistingUUID), general(GLOBAL))
        }).when(agentService).deleteAgents(eq(singletonList(nonExistingUUID)), any() as HttpOperationResult)

        def requestBody = ["uuids": [nonExistingUUID]]
        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Not Found { Agent 'non-existing-uuid' not found }")
      }

      @Test
      void 'delete agents should throw 406 when called with list of UUIDs containing non disabled agent'() {
        loginAsAdmin()

        def disabledAgent = disabled()
        def building = building()

        when(agentService.findAgent(disabledAgent.getUuid())).thenReturn(disabledAgent)
        when(agentService.findAgent(building.getUuid())).thenReturn(building)

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.notAcceptable("Could not delete any agents, as one or more agents might not be disabled or are still building.", general(GLOBAL))
        }).when(agentService).deleteAgents(eq(asList(disabledAgent.getUuid(), building.getUuid())), any() as HttpOperationResult)

        def requestBody = ["uuids": [disabledAgent.getUuid(), building.getUuid()]]
        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .hasStatus(406)
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Could not delete any agents, as one or more agents might not be disabled or are still building.")
      }

      @Test
      void 'delete agents should throw 406 when called with list of single non disabled UUID'() {
        loginAsAdmin()

        def building = building()

        when(agentService.findAgent(building.getUuid())).thenReturn(building)

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.notAcceptable("Failed to delete an agent, as it is not in a disabled state or is still building.", general(GLOBAL))
        }).when(agentService).deleteAgents(eq(singletonList(building.getUuid())), any() as HttpOperationResult)

        def requestBody = ["uuids": [building.getUuid()]]
        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .hasStatus(406)
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Failed to delete an agent, as it is not in a disabled state or is still building.")
      }

      @Test
      void 'should render result in case of internal server error'() {
        loginAsAdmin()

        doAnswer({ InvocationOnMock invocation ->
          def result = invocation.getArgument(1) as HttpOperationResult
          result.internalServerError("Some error description of why deleting agents failed", null)
        }).when(agentService).deleteAgents(eq(asList("agent-1", "agent-2")), any() as HttpOperationResult)

        def requestBody = ["uuids": ["agent-1", "agent-2"]]

        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isInternalServerError()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Some error description of why deleting agents failed")
      }
    }
  }
}
