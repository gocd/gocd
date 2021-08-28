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
package com.thoughtworks.go.apiv7.agents

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.EnvironmentConfig
import com.thoughtworks.go.config.EnvironmentsConfig
import com.thoughtworks.go.config.exceptions.BadRequestException
import com.thoughtworks.go.config.exceptions.EntityType
import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.domain.AgentInstance
import com.thoughtworks.go.domain.NullAgentInstance
import com.thoughtworks.go.domain.exception.InvalidAgentInstructionException
import com.thoughtworks.go.server.domain.AgentInstances
import com.thoughtworks.go.server.service.AgentService
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.TriState
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import java.util.stream.Stream

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.util.HaltApiMessages.confirmHeaderMissing
import static com.thoughtworks.go.helper.AgentInstanceMother.*
import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment
import static com.thoughtworks.go.helper.EnvironmentConfigMother.remote
import static java.util.Arrays.asList
import static java.util.Collections.*
import static java.util.stream.Collectors.toSet
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

@MockitoSettings(strictness = Strictness.LENIENT)
class AgentsControllerV7Test implements SecurityServiceTrait, ControllerTrait<AgentsControllerV7> {
  @Mock
  private AgentService agentService

  @Mock
  private EnvironmentConfigService environmentConfigService

  @Override
  AgentsControllerV7 createControllerInstance() {
    return new AgentsControllerV7(agentService, new ApiAuthenticationHelper(securityService, goConfigService), securityService, environmentConfigService)
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
      def instance = idle()
      instance.getAgent().setEnvironments("env1,env2,unknown-env")

      def instances = new AgentInstances(null, null, instance)
      when(agentService.getAgentInstances()).thenReturn(instances)

      def environmentConfigs = new HashSet<EnvironmentConfig>()
      environmentConfigs.add(environment("env1"))
      environmentConfigs.add(environment("env2"))
      when(environmentConfigService.getAgentEnvironments(instance.getUuid())).thenReturn(environmentConfigs)

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
              "agent_version"     : "UNKNOWN",
              "agent_bootstrapper_version" : "UNKNOWN",
              "resources"         : [],
              "environments"      : [
                [
                  name  : "env1",
                  origin: [
                    type    : "gocd",
                    "_links": [
                      "self": [
                        "href": "http://test.host/go/admin/config_xml"
                      ],
                      "doc" : [
                        "href": apiDocsUrl("#get-configuration")
                      ]
                    ]
                  ]
                ],
                [
                  name  : "env2",
                  origin: [
                    type    : "gocd",
                    "_links": [
                      "self": [
                        "href": "http://test.host/go/admin/config_xml"
                      ],
                      "doc" : [
                        "href": apiDocsUrl("#get-configuration")
                      ]
                    ]
                  ]
                ],
                [
                  name  : "unknown-env",
                  origin: [
                    type: "unknown"
                  ]
                ]
              ],
              "build_state"       : "Idle"
            ]
          ]
        ]
      ])
    }

    @Test
    void "should return an empty list of agents if there are no agents available"() {
      def mockAgentInstances = mock(AgentInstances.class)
      when(agentService.getAgentInstances()).thenReturn(mockAgentInstances)

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
          "agents": []
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
      def agentInstance = idle()
      agentInstance.getAgent().setEnvironments("env1,env2,unknown-env")
      when(agentService.findAgent("uuid2")).thenReturn(agentInstance)
      def environments = Stream.of(environment("env1"), environment("env2")).collect(toSet())
      when(environmentConfigService.getAgentEnvironments("uuid2")).thenReturn(environments)

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
        "agent_version"     : "UNKNOWN",
        "agent_bootstrapper_version" : "UNKNOWN",
        "resources"         : [],
        "environments"      : [
          [
            name  : "env1",
            origin: [
              type    : "gocd",
              "_links": [
                "self": [
                  "href": "http://test.host/go/admin/config_xml"
                ],
                "doc" : [
                  "href": apiDocsUrl("#get-configuration")
                ]
              ]
            ]
          ],
          [
            name  : "env2",
            origin: [
              type    : "gocd",
              "_links": [
                "self": [
                  "href": "http://test.host/go/admin/config_xml"
                ],
                "doc" : [
                  "href": apiDocsUrl("#get-configuration")
                ]
              ]
            ]
          ],
          [
            name  : "unknown-env",
            origin: [
              type: "unknown"
            ]
          ]
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
      AgentInstance updatedAgentInstance = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", asList("psql", "java"), "20.3.0-1234", "20.5.0-2345")
      updatedAgentInstance.getAgent().setEnvironments("env1,unknown-env")

      def envsConfig = new EnvironmentsConfig()
      def envConfig = environment("env1")
      when(environmentConfigService.find("env1")).thenReturn(envConfig)
      when(environmentConfigService.getAgentEnvironments("uuid2")).thenReturn(singleton(envConfig))

      envsConfig.add(envConfig)
      when(agentService.updateAgentAttributes(
        eq("uuid2"),
        eq("agent02.example.com"),
        eq("java,psql"),
        eq("env1,unknown-env"),
        eq(TriState.TRUE)
      )
      ).thenReturn(updatedAgentInstance)

      def requestBody = ["hostname"          : "agent02.example.com",
                         "agent_config_state": "Enabled",
                         "resources"         : ["java", "psql"],
                         "environments"      : ["env1", "unknown-env"]
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
        "agent_version"     : "20.5.0-2345",
        "agent_bootstrapper_version" : "20.3.0-1234",
        "resources"         : ["java", "psql"],
        "environments"      : [
          [
            name  : "env1",
            origin: [
              type    : "gocd",
              "_links": [
                "self": [
                  "href": "http://test.host/go/admin/config_xml"
                ],
                "doc" : [
                  "href": apiDocsUrl("#get-configuration")
                ]
              ]
            ]
          ],
          [
            name  : "unknown-env",
            origin: [
              type: "unknown"
            ]
          ]
        ],
        "build_state"       : "Idle"
      ])
    }

    @Test
    void 'should throw 400 - bad request when there is no operation specified in the request to be performed on agent'() {
      loginAsAdmin()

      doAnswer({ InvocationOnMock invocation ->
        def msg = "Bad Request. No operation is specified in the request to be performed on agent."
        throw new BadRequestException(msg)
      }).when(agentService).updateAgentAttributes(
        eq("uuid2"),
        eq(null),
        eq(null),
        eq(null),
        eq(TriState.UNSET))

      def requestBody = ""
      patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)

      assertThatResponse()
        .isBadRequest()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Bad Request. No operation is specified in the request to be performed on agent.")
    }

    @Test
    void 'should reset agents environment attribute value to null in db when environments is specified as empty string in the request payload'() {
      loginAsAdmin()
      def resources = asList("psql", "java")
      AgentInstance agentWithoutEnvs = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", resources, "20.3.0-1234", "20.5.0-2345")

      when(environmentConfigService.getAgentEnvironments("uuid2")).thenReturn(emptySet())
      when(agentService.updateAgentAttributes(
        eq("uuid2"),
        eq("agent02.example.com"),
        eq("java,psql"),
        eq(""),
        eq(TriState.TRUE)
      )
      ).thenReturn(agentWithoutEnvs)

      def requestBody = ["hostname"          : "agent02.example.com",
                         "agent_config_state": "Enabled",
                         "resources"         : ["java", "psql"],
                         "environments"      : []
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
        "agent_version"     : "20.5.0-2345",
        "agent_bootstrapper_version" : "20.3.0-1234",
        "resources"         : ["java", "psql"],
        "environments"      : [],
        "build_state"       : "Idle"
      ])
    }

    @Test
    void 'should reset agents resources attribute value to null in db when resources is specified as empty array in the request payload'() {
      loginAsAdmin()

      def resources = emptyList()
      AgentInstance agentWithoutEnvsAndResources = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", resources, "20.3.0-1234", "20.5.0-2345")

      def emptyEnvsConfig = new EnvironmentsConfig()
      when(environmentConfigService.getAgentEnvironments("uuid2")).thenReturn(emptySet())
      when(agentService.updateAgentAttributes(
        eq("uuid2"),
        eq("agent02.example.com"),
        eq(""),
        eq(""),
        eq(TriState.TRUE)
      )
      ).thenReturn(agentWithoutEnvsAndResources)

      def requestBody = ["hostname"          : "agent02.example.com",
                         "agent_config_state": "Enabled",
                         "resources"         : [],
                         "environments"      : []
      ]
      patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)

      println "\n\nResponse is \n\n" + response

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
        "agent_version"     : "20.5.0-2345",
        "agent_bootstrapper_version" : "20.3.0-1234",
        "resources"         : [],
        "environments"      : [],
        "build_state"       : "Idle"
      ])
    }

    @Test
    void 'should error out with 500 code when there is internal server error'() {
      loginAsAdmin()
      when(agentService.updateAgentAttributes(anyString(), anyString(), anyString(), anyString(), any() as TriState))
        .thenAnswer({ InvocationOnMock invocation -> throw new Exception("Oops! something went wrong!")
      })

      def requestBody = [
        "hostname"          : "agent02.example.com",
        "agent_config_state": "",
        "resources"         : "Java,Linux",
        "environments"      : ["Foo"]
      ]

      patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)
      assertThatResponse()
        .isInternalServerError()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Oops! something went wrong!")
    }

    @Test
    void 'should error out with 400 code when no operation is performed on agent'() {
      loginAsAdmin()
      when(agentService.updateAgentAttributes(anyString(), anyString(), anyString(), anyString(), any() as TriState))
        .thenAnswer({ InvocationOnMock invocation -> throw new BadRequestException("Bad Request. No operation performed on agent!")
      })

      def requestBody = [
        "hostname"          : "agent02.example.com",
        "agent_config_state": "",
        "resources"         : "Java,Linux",
        "environments"      : ["Foo"]
      ]

      patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)
      assertThatResponse()
        .isBadRequest()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Bad Request. No operation performed on agent!")
    }

    @Test
    void 'should error out with 404 code when agent being updated is not found'() {
      loginAsAdmin()
      when(agentService.updateAgentAttributes(anyString(), anyString(), anyString(), anyString(), any() as TriState))
        .thenAnswer({ InvocationOnMock invocation -> throw new RecordNotFoundException(EntityType.Agent, "uuid2")
      })

      def requestBody = [
        "hostname"          : "agent02.example.com",
        "agent_config_state": "",
        "resources"         : "Java,Linux",
        "environments"      : ["Foo"]
      ]

      patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)
      assertThatResponse()
        .isNotFound()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Agent with uuid 'uuid2' was not found!")
    }

    @Test
    void 'should error out with 422 code when update fails due to agent validation error'() {
      loginAsAdmin()
      when(agentService.updateAgentAttributes(anyString(), anyString(), anyString(), anyString(), any() as TriState))
        .thenAnswer({ InvocationOnMock invocation ->
        def agentInstance = agentWithConfigErrors()
        return agentInstance
      })

      def requestBody = [
        "hostname"          : "agent02.example.com",
        "agent_config_state": "",
        "resources"         : "Java,Linux",
        "environments"      : ["Foo"]
      ]

      patchWithApiHeader(controller.controllerPath("/uuid"), requestBody)
      assertThatResponse()
        .isUnprocessableEntity()
        .hasContentType(controller.mimeType)
        .hasJsonBody([
        "message": "Updating agent failed.",
        "data"   : [
          "uuid"              : "uuid",
          "hostname"          : "host",
          "ip_address"        : "IP",
          "sandbox"           : "",
          "operating_system"  : "",
          "free_space"        : "unknown",
          "agent_config_state": "Enabled",
          "agent_state"       : "Missing",
          "agent_version"     : "UNKNOWN",
          "agent_bootstrapper_version" : "UNKNOWN",
          "resources"         : ["bar\$", "foo%"],
          "environments"      : [],
          "build_state"       : "Unknown",
          "errors"            : [
            "ip_address": ["'IP' is an invalid IP address."],
            "resources" : [
              "Resource name 'foo%' is not valid. Valid names much match '^[-\\w\\s|.]*\$\'",
              "Resource name 'bar\$' is not valid. Valid names much match '^[-\\w\\s|.]*\$\'"
            ]
          ]
        ]
      ])
    }

    @Nested
    class Environments {
      @Test
      void 'should pass empty environments string to service given empty comma separated list of environments'() {
        loginAsAdmin()
        AgentInstance updatedAgentInstance = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", asList("psql", "java"), "20.3.0-1234", "20.5.0-2345")

        def commaSeparatedEnvs = "             "

        when(agentService.updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(""),
          eq(TriState.TRUE)
        )
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
          eq(""),
          eq(TriState.TRUE)
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
          "agent_version"     : "20.5.0-2345",
          "agent_bootstrapper_version" : "20.3.0-1234",
          "resources"         : ["java", "psql"],
          "environments"      : [],
          "build_state"       : "Idle"
        ])
      }

      @Test
      void 'should pass null as environments string to service given null comma separated list of environments'() {
        loginAsAdmin()
        AgentInstance updatedAgentInstance = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", asList("psql", "java"), "20.3.0-1234", "20.5.0-2345")
        when(agentService.updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          isNull() as String,
          eq(TriState.TRUE))
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
          isNull() as String,
          eq(TriState.TRUE)
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
          "agent_version"     : "20.5.0-2345",
          "agent_bootstrapper_version" : "20.3.0-1234",
          "resources"         : ["java", "psql"],
          "environments"      : [],
          "build_state"       : "Idle"
        ])
      }

      @Test
      void 'should filter out environments which are associated via config-repo'() {
        loginAsAdmin()
        AgentInstance updatedAgentInstance = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", asList("psql", "java"), "20.3.0-1234", "20.5.0-2345")
        updatedAgentInstance.getAgent().setEnvironments("env1,config-repo-env")

        def localEnvName = "env1"
        def remoteEnvName = "config-repo-env"
        def envConfig = environment(localEnvName)
        def remoteEnvConfig = remote(remoteEnvName)
        def configRepoConfig = mock(ConfigRepoConfig.class)
        remoteEnvConfig.setOrigins(new RepoConfigOrigin(configRepoConfig, "revision"))
        remoteEnvConfig.addAgent("uuid2")
        def envsConfig = new HashSet<EnvironmentConfig>()
        envsConfig.add(envConfig)
        envsConfig.add(remoteEnvConfig)
        when(environmentConfigService.find(localEnvName)).thenReturn(envConfig)
        when(environmentConfigService.find(remoteEnvName)).thenReturn(remoteEnvConfig)
        when(environmentConfigService.getAgentEnvironments("uuid2")).thenReturn(envsConfig)
        when(configRepoConfig.id).thenReturn("id")

        when(agentService.updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(localEnvName),
          eq(TriState.TRUE)
        )
        ).thenReturn(updatedAgentInstance)

        def requestBody = ["hostname"          : "agent02.example.com",
                           "agent_config_state": "Enabled",
                           "resources"         : ["java", "psql"],
                           "environments"      : [localEnvName, remoteEnvName]
        ]
        patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)

        verify(agentService).updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(localEnvName),
          eq(TriState.TRUE)
        )

        def expectedJson = [
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
          "agent_version"     : "20.5.0-2345",
          "agent_bootstrapper_version" : "20.3.0-1234",
          "resources"         : ["java", "psql"],
          "environments"      : [
            [
              "name"  : "config-repo-env",
              "origin": [
                "type"  : "config-repo",
                "_links": [
                  "self": [
                    "href": "http://test.host/go/api/admin/config_repos/id"
                  ],
                  "doc" : [
                    "href": apiDocsUrl("#config-repos")
                  ],
                  "find": [
                    "href": "http://test.host/go/api/admin/config_repos/:id"
                  ]
                ]
              ]
            ], [
              "name"  : "env1",
              "origin": [
                "type"  : "gocd",
                "_links": [
                  "self": [
                    "href": "http://test.host/go/admin/config_xml"
                  ],
                  "doc" : [
                    "href": apiDocsUrl("#get-configuration")
                  ]
                ]
              ]
            ]
          ],
          "build_state"       : "Idle"
        ]

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
      }

      @Test
      void 'should pass in the env name even if it is not defined'() {
        loginAsAdmin()
        AgentInstance updatedAgentInstance = idleWith("uuid2", "agent02.example.com", "10.0.0.1", "/var/lib/bar", 10, "", asList("psql", "java"), "20.3.0-1234", "20.5.0-2345")
        updatedAgentInstance.getAgent().setEnvironments("env1,non-existent-env")

        def localEnvName = "env1"
        def undefinedEnvName = "non-existent-env"
        def envConfig = environment(localEnvName)
        def envsConfig = new HashSet<EnvironmentConfig>()
        envsConfig.add(envConfig)
        when(environmentConfigService.find(localEnvName)).thenReturn(envConfig)
        when(environmentConfigService.getAgentEnvironments("uuid2")).thenReturn(envsConfig)

        def envs = "$localEnvName,$undefinedEnvName".toString()
        when(agentService.updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(envs),
          eq(TriState.TRUE)
        )
        ).thenReturn(updatedAgentInstance)

        def requestBody = ["hostname"          : "agent02.example.com",
                           "agent_config_state": "Enabled",
                           "resources"         : ["java", "psql"],
                           "environments"      : [localEnvName, undefinedEnvName]
        ]
        patchWithApiHeader(controller.controllerPath("/uuid2"), requestBody)

        verify(agentService).updateAgentAttributes(
          eq("uuid2"),
          eq("agent02.example.com"),
          eq("java,psql"),
          eq(envs),
          eq(TriState.TRUE)
        )

        def expectedJson = [
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
          "agent_version"     : "20.5.0-2345",
          "agent_bootstrapper_version" : "20.3.0-1234",
          "resources"         : ["java", "psql"],
          "environments"      : [
            [
              "name"  : "env1",
              "origin": [
                "type"  : "gocd",
                "_links": [
                  "self": [
                    "href": "http://test.host/go/admin/config_xml"
                  ],
                  "doc" : [
                    "href": apiDocsUrl("#get-configuration")
                  ]
                ]
              ]
            ],
            [
              name  : undefinedEnvName,
              origin: [
                type: "unknown"
              ]
            ]
          ],
          "build_state"       : "Idle"
        ]

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonBody(expectedJson)
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
      doNothing().when(agentService).bulkUpdateAgentAttributes(
        any() as List<String>,
        any() as List<String>,
        any() as List<String>,
        any() as List<String>,
        any() as List<String>,
        any() as TriState,
        any() as EnvironmentConfigService
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
        .hasJsonMessage("Updated agent(s) with uuid(s): [adb9540a-b954-4571-9d9b-2f330739d4da, adb528b2-b954-1234-9d9b-b27ag4h568e1].")
    }

    @Test
    void 'should throw 400 - bad request when no operation is specified in the request to be performed on agents'() {
      loginAsAdmin()
      def uuids = [
        "adb9540a-b954-4571-9d9b-2f330739d4da",
        "adb528b2-b954-1234-9d9b-b27ag4h568e1"
      ] as List<String>

      doAnswer({ InvocationOnMock invocation ->
        throw new BadRequestException("Bad Request. No operation is specified in the request to be performed on agents.")
      }).when(agentService).bulkUpdateAgentAttributes(
        eq(uuids) as List<String>,
        eq(emptyList()) as List<String>,
        eq(emptyList()) as List<String>,
        eq(emptyList()) as List<String>,
        eq(emptyList()) as List<String>,
        eq(TriState.UNSET) as TriState,
        any() as EnvironmentConfigService
      )

      def requestBody = [
        "uuids": uuids
      ]
      patchWithApiHeader(controller.controllerPath(), requestBody)
      assertThatResponse()
        .isBadRequest()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Bad Request. No operation is specified in the request to be performed on agents.")
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

      def uuid = "uuid"
      when(agentService.findAgent(uuid)).thenReturn(idle())
      doNothing().when(agentService).deleteAgents(eq(asList(uuid)))

      deleteWithApiHeader(controller.controllerPath(uuid))

      assertThatResponse()
        .isOk()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Deleted 1 agent(s).")
    }

    @Test
    void 'delete agent should throw 404 when called with UUID that does not exist'() {
      loginAsAdmin()

      def nonExistingUUID = "non-existing-uuid"

      when(agentService.findAgent(nonExistingUUID)).thenReturn(new NullAgentInstance(nonExistingUUID))
      doAnswer({ InvocationOnMock invocation ->
        throw new RecordNotFoundException(EntityType.Agent, nonExistingUUID)
      }).when(agentService).deleteAgents(eq(singletonList(nonExistingUUID)))

      deleteWithApiHeader(controller.controllerPath(nonExistingUUID))

      assertThatResponse()
        .isNotFound()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Agent with uuid 'non-existing-uuid' was not found!")
    }

    @Test
    void 'should throw 422 in case of agent(s) can not be deleted'() {
      loginAsAdmin()

      doAnswer({ InvocationOnMock invocation ->
        throw new UnprocessableEntityException("Some message")
      }).when(agentService).deleteAgents(eq(asList("uuid2")))

      deleteWithApiHeader(controller.controllerPath("uuid2"))

      assertThatResponse()
        .isUnprocessableEntity()
        .hasContentType(controller.mimeType)
        .hasJsonMessage("Your request could not be processed. Some message")
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

        doNothing().when(agentService).deleteAgents(eq(asList("agent-1", "agent-2")))

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

        doNothing().when(agentService).deleteAgents(eq(asList(disabledUUID1, disabledUUID2)))

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

        doNothing().when(agentService).deleteAgents(eq(emptyList()))

        def requestBody = ""
        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Deleted 0 agent(s).")
      }

      @Test
      void 'should delete agents with uuids when empty list of UUIDs is passed'() {
        loginAsAdmin()

        doNothing().when(agentService).deleteAgents(eq(emptyList()))

        def requestBody = ["uuids": []]
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
          throw new RecordNotFoundException(EntityType.Agent, nonExistingUUID)
        }).when(agentService).deleteAgents(eq(singletonList(nonExistingUUID)))

        def requestBody = ["uuids": [nonExistingUUID]]
        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Agent with uuid 'non-existing-uuid' was not found!")
      }

      @Test
      void 'delete agents should throw 406 when called with list of UUIDs containing non disabled agent'() {
        loginAsAdmin()

        def disabledAgent = disabled()
        def building = building()

        when(agentService.findAgent(disabledAgent.getUuid())).thenReturn(disabledAgent)
        when(agentService.findAgent(building.getUuid())).thenReturn(building)

        doAnswer({ InvocationOnMock invocation ->
          throw new UnprocessableEntityException("Could not delete any agents, as one or more agents might not be disabled or are still building.")
        }).when(agentService).deleteAgents(eq(asList(disabledAgent.getUuid(), building.getUuid())))

        def requestBody = ["uuids": [disabledAgent.getUuid(), building.getUuid()]]
        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Your request could not be processed. Could not delete any agents, as one or more agents might not be disabled or are still building.")
      }

      @Test
      void 'delete agents should throw 406 when called with list of single non disabled UUID'() {
        loginAsAdmin()

        def building = building()

        when(agentService.findAgent(building.getUuid())).thenReturn(building)

        doAnswer({ InvocationOnMock invocation ->
          throw new UnprocessableEntityException("Failed to delete an agent, as it is not in a disabled state or is still building.")
        }).when(agentService).deleteAgents(eq(singletonList(building.getUuid())))

        def requestBody = ["uuids": [building.getUuid()]]
        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Your request could not be processed. Failed to delete an agent, as it is not in a disabled state or is still building.")
      }

      @Test
      void 'should render result in case of internal server error'() {
        loginAsAdmin()

        doAnswer({ InvocationOnMock invocation ->
          throw new RuntimeException("Error deleting agents", null)
        }).when(agentService).deleteAgents(eq(asList("agent-1", "agent-2")))

        def requestBody = ["uuids": ["agent-1", "agent-2"]]

        deleteWithApiHeader(controller.controllerPath(), requestBody)

        assertThatResponse()
          .isInternalServerError()
          .hasContentType(controller.mimeType)
          .hasJsonMessage("Shoot! This is unexpected. Something went wrong while deleting agent(s)! More details : Error deleting agents")
      }
    }
  }

  @Nested
  class killRunningTasks {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return 'killRunningTasks'
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath('agent_uuid', 'kill_running_tasks'), ['X-GoCD-Confirm': 'true'])
      }
    }

    @Nested
    class AsAdmin {
      @Test
      void 'should accept instruction to kill all running tasks on agent'() {
        enableSecurity()
        loginAsAdmin()

        postWithApiHeader(controller.controllerPath('agent_uuid', 'kill_running_tasks'), ['x-gocd-confirm': 'true'], null)

        verify(agentService).killAllRunningTasksOnAgent('agent_uuid')
        assertThatResponse()
          .isAccepted()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 404 if agent does not exist'() {
        enableSecurity()
        loginAsAdmin()

        when(agentService.killAllRunningTasksOnAgent("agent_uuid")).thenThrow(new RecordNotFoundException("Agent does not exist."))

        postWithApiHeader(controller.controllerPath('agent_uuid', 'kill_running_tasks'), ['x-gocd-confirm': 'true'], null)

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Agent does not exist.")
      }

      @Test
      void 'should return 409 if agent cannot be instructed to kill tasks'() {
        enableSecurity()
        loginAsAdmin()

        when(agentService.killAllRunningTasksOnAgent("agent_uuid")).thenThrow(new InvalidAgentInstructionException("Agent is idle."))

        postWithApiHeader(controller.controllerPath('agent_uuid', 'kill_running_tasks'), ['x-gocd-confirm': 'true'], null)

        assertThatResponse()
          .isConflict()
          .hasJsonMessage("Agent is idle.")
      }

      @Nested
      class CORS {
        @Test
        void 'bails if confirm header is missing'() {
          enableSecurity()
          loginAsAdmin()
          postWithApiHeader(controller.controllerPath('agent_uuid', 'kill_running_tasks'), null)
          assertThatResponse()
            .isBadRequest()
            .hasContentType(controller.mimeType)
            .hasJsonMessage(confirmHeaderMissing())
        }
      }
    }
  }

}
