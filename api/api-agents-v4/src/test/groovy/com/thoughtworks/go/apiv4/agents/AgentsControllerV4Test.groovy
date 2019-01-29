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

package com.thoughtworks.go.apiv4.agents

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.domain.AgentInstance
import com.thoughtworks.go.domain.NullAgentInstance
import com.thoughtworks.go.server.domain.Username
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
import static com.thoughtworks.go.helper.AgentInstanceMother.idle
import static com.thoughtworks.go.helper.AgentInstanceMother.idleWith
import static java.util.Arrays.asList
import static java.util.Collections.singleton
import static java.util.stream.Collectors.toSet
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.doAnswer
import static org.mockito.Mockito.when
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
      when(agentService.agentEnvironmentMap()).thenReturn(new HashMap<AgentInstance, Collection<String>>() {
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
        .hasJsonMessage(HaltApiMessages.notFoundMessage())
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

      when(environmentConfigService.environmentsFor("uuid2")).thenReturn(singleton("env1"))
      when(agentService.updateAgentAttributes(
        eq(currentUsername()),
        any() as HttpOperationResult,
        eq("uuid2"),
        eq("agent02.example.com"),
        eq("java,psql"),
        eq("env1"),
        eq(TriState.TRUE))
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
      when(agentService.updateAgentAttributes(any() as Username, any() as HttpOperationResult, anyString(), anyString(), anyString(), anyString(), any() as TriState)).thenAnswer({ InvocationOnMock invocation ->
        def result = invocation.getArgument(1) as HttpOperationResult
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
        def result = invocation.getArgument(1) as HttpLocalizedOperationResult
        result.setMessage("Updated agent(s) with uuid(s): [agent-1, agent-2].")

      }).when(agentService).bulkUpdateAgentAttributes(
        eq(currentUsername()) as Username,
        any() as LocalizedOperationResult,
        any() as List<String>,
        any() as List<String>,
        any() as List<String>,
        any() as List<String>,
        any() as List<String>,
        any() as TriState
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

    @Test
    void 'should delete agent with given uuid'() {
      loginAsAdmin()

      when(agentService.findAgent("uuid2")).thenReturn(idle())
      doAnswer({ InvocationOnMock invocation ->
        def result = invocation.getArgument(1) as HttpOperationResult
        result.ok("Deleted 1 agent(s).")
      }).when(agentService).deleteAgents(eq(currentUsername()), any() as HttpOperationResult, eq(asList("uuid2")))

      deleteWithApiHeader(controller.controllerPath("uuid2"))

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Deleted 1 agent(s).")
    }

    @Test
    void 'should render result in case of error'() {
      loginAsAdmin()

      doAnswer({ InvocationOnMock invocation ->
        def result = invocation.getArgument(1) as HttpOperationResult
        def message = "Failed to delete agent."
        result.unprocessibleEntity(message, "Some description", null)
      }).when(agentService).deleteAgents(eq(currentUsername()), any() as HttpOperationResult, eq(asList("uuid2")))

      deleteWithApiHeader(controller.controllerPath("uuid2"))

      assertThatResponse()
        .isUnprocessableEntity()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Failed to delete agent. { Some description }")
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

    @Test
    void 'should delete agents with uuids'() {
      loginAsAdmin()

      when(agentService.findAgent("agent-1")).thenReturn(idleWith("agent-1"))
      when(agentService.findAgent("agent-2")).thenReturn(idleWith("agent-2"))

      doAnswer({ InvocationOnMock invocation ->
        def result = invocation.getArgument(1) as HttpOperationResult
        result.ok("Deleted 2 agent(s).")
      }).when(agentService).deleteAgents(eq(currentUsername()), any() as HttpOperationResult, eq(asList("agent-1", "agent-2")))

      def requestBody = ["uuids": ["agent-1", "agent-2"]]

      deleteWithApiHeader(controller.controllerPath(), requestBody)

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Deleted 2 agent(s).")
    }

    @Test
    void 'should render result in case of error'() {
      loginAsAdmin()

      doAnswer({ InvocationOnMock invocation ->
        def result = invocation.getArgument(1) as HttpOperationResult
        def message = "Failed to delete agent."
        result.unprocessibleEntity(message, "Some description", null)
      }).when(agentService).deleteAgents(eq(currentUsername()), any() as HttpOperationResult, eq(asList("agent-1", "agent-2")))

      def requestBody = ["uuids": ["agent-1", "agent-2"]]

      deleteWithApiHeader(controller.controllerPath(), requestBody)

      assertThatResponse()
        .isUnprocessableEntity()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Failed to delete agent. { Some description }")
    }
  }
}
