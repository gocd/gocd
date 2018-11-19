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
import com.thoughtworks.go.server.service.AgentService
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import java.util.stream.Stream

import static com.thoughtworks.go.helper.AgentInstanceMother.idle
import static java.util.stream.Collectors.toSet
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
          put(idle(), Arrays.asList("env1", "env2"))
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
            "href": "https://api.gocd.org/current/#agents"
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
                  "href": "https://api.gocd.org/current/#agents"
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
              "free_space"        : "10.0 KB",
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
            "href": "https://api.gocd.org/current/#agents"
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
        "free_space"        : "10.0 KB",
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
        getWithApiHeader(controller.controllerPath())
      }
    }
  }

  @Nested
  class BulkUpdate {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        //TODO:
        return 'update'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }
  }

  @Nested
  class Delete {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'update'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }
  }

  @Nested
  class BulkDelete {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'update'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }
  }
}
