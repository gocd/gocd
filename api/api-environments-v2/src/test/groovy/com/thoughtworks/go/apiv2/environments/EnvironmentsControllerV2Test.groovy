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

package com.thoughtworks.go.apiv2.environments

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv2.environments.representers.EnvironmentRepresenter
import com.thoughtworks.go.config.BasicEnvironmentConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.EnvironmentConfig
import com.thoughtworks.go.domain.ConfigElementForEdit
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class EnvironmentsControllerV2Test implements SecurityServiceTrait, ControllerTrait<EnvironmentsControllerV2> {

  @Mock
  EnvironmentConfigService environmentConfigService

  @Mock
  EntityHashingService entityHashingService

  @Override
  EnvironmentsControllerV2 createControllerInstance() {
    new EnvironmentsControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), environmentConfigService, entityHashingService)

  }

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Nested
  class Show {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'show'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("env1"))
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should return a environment with given name'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult.class))).thenReturn(new ConfigElementForEdit(env1, "3123abcef"))

        getWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse().isOk().hasJsonBody([
          "_links"               : [
            "doc" : [
              "href": "https://api.go.cd/current/#environment-config"
            ],
            "find": [
              "href": "http://test.host/go/api/admin/environments/:name"
            ],
            "self": [
              "href": "http://test.host/go/api/admin/environments/env1"
            ]
          ],
          "agents"               : [
            [
              "_links": [
                "doc" : [
                  "href": "https://api.gocd.org/current/#agents"
                ],
                "find": [
                  "href": "/api/agents/:uuid"
                ],
                "self": [
                  "href": "http://test.host/go/api/agents/agent1"
                ]
              ],
              "uuid"  : "agent1"
            ],
            [
              "_links": [
                "doc" : [
                  "href": "https://api.gocd.org/current/#agents"
                ],
                "find": [
                  "href": "/api/agents/:uuid"
                ],
                "self": [
                  "href": "http://test.host/go/api/agents/agent2"
                ]
              ],
              "uuid"  : "agent2"
            ]
          ],
          "environment_variables": [
            [
              "name"  : "JAVA_HOME",
              "secure": false,
              "value" : "/bin/java"
            ]
          ],
          "name"                 : "env1",
          "pipelines"            : [
            [
              "_links": [
                "doc" : [
                  "href": "https://api.go.cd/current/#pipelines"
                ],
                "find": [
                  "href": "/api/admin/pipelines/:pipeline_name"
                ],
                "self": [
                  "href": "http://test.host/go/api/pipelines/Pipeline1/history"
                ]
              ],
              "name"  : "Pipeline1"
            ],
            [
              "_links": [
                "doc" : [
                  "href": "https://api.go.cd/current/#pipelines"
                ],
                "find": [
                  "href": "/api/admin/pipelines/:pipeline_name"
                ],
                "self": [
                  "href": "http://test.host/go/api/pipelines/Pipeline2/history"
                ]
              ],
              "name"  : "Pipeline2"
            ]
          ]
        ])
      }

      @Test
      void 'should return error when environment with specified name is not found'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult.class)))
          .then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.getArguments().last()
          result.badRequest("No such environment")
        })

        getWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("No such environment")
      }
    }
  }


  @Nested
  class Remove {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'remove'
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath("env1"))
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }


      @Test
      void 'should delete environment with specified name'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        when(environmentConfigService.getMergedEnvironmentforDisplay(anyString(), any(HttpLocalizedOperationResult.class))).thenReturn(new ConfigElementForEdit(env1, "3123abcef"))
        when(environmentConfigService.deleteEnvironment(eq(env1), eq(currentUsername()), any(HttpLocalizedOperationResult))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.getArguments().last()
            result.setMessage("Environment 'my_environment' was deleted successfully.")
        })

        deleteWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse()
          .isOk()
          .hasJsonMessage("Environment 'my_environment' was deleted successfully.")
      }

      @Test
      void 'should error out if the environment doesnt exist'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(anyString(), any(HttpLocalizedOperationResult.class)))
          .then({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.getArguments().last()
          result.badRequest("No such environment")
        })

        getWithApiHeader(controller.controllerPath("env1"))

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("No such environment")
      }
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
        putWithApiHeader(controller.controllerPath("env1"), [])
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }


      @Test
      void 'should update environment'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))


        when(entityHashingService.md5ForEntity(env1)).thenReturn("ffff")

        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult)))
        .thenReturn(new ConfigElementForEdit<>(env1, "ffff"))

        when(environmentConfigService.updateEnvironment(eq("env1"), eq(env1), eq(currentUsername()), anyString(), any(HttpLocalizedOperationResult))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Updated environment '\" + oldEnvironmentConfigName + \"'.")
        })

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, env1) })

        putWithApiHeader(controller.controllerPath("env1"),['if-match': 'ffff'], json)

        assertThatResponse()
          .isOk()
          .hasJsonBody(json)
      }

      @Test
      void 'should error out if the environment doesnt exist'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult)))
        .then({
          InvocationOnMock invocation ->
            def result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.badRequest("The environment does not exist")
        })
        putWithApiHeader(controller.controllerPath("env1"), [name: "env1"])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The environment does not exist")
      }
    }
  }


  @Nested
  class PartialUpdate {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'partialUpdate'
      }

      @Override
      void makeHttpCall() {
        patchWithApiHeader(controller.controllerPath("env1"), [])
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }


      @Test
      void 'should update attributes of environment'() {
        def oldConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        oldConfig.addAgent("agent1")
        oldConfig.addAgent("agent2")
        oldConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        oldConfig.addEnvironmentVariable("URL", "google.com")
        oldConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))
        oldConfig.addPipeline(new CaseInsensitiveString("Pipeline2"))
        oldConfig.addPipeline(new CaseInsensitiveString("Pipeline3"))


        def updatedConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        updatedConfig.addAgent("agent1")
        updatedConfig.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        updatedConfig.addPipeline(new CaseInsensitiveString("Pipeline1"))
        updatedConfig.addPipeline(new CaseInsensitiveString("Pipeline2"))


        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult)))
          .thenReturn(new ConfigElementForEdit<>(oldConfig, "old_md5_hash"))
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult)))
          .thenReturn(new ConfigElementForEdit<>(updatedConfig, "new_md5_hash"))

        when(environmentConfigService.patchEnvironment(
          eq(oldConfig), anyList(), anyList(), anyList(), anyList(),anyList(), anyList(),eq(currentUsername()), any(HttpLocalizedOperationResult))
        ).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Updated environment '\" + oldEnvironmentConfigName + \"'.")
        })


        patchWithApiHeader(controller.controllerPath("env1"), [
          "pipelines": [
            "add": [
              "Pipeline1", "Pipeline2"
            ],
            "remove": [
              "Pipeline3"
            ]
          ],
          "agents": [
            "add": [
              "agent1"
            ],
            "remove": [
              "agent2"
            ]
          ],
          "environment_variables": [
            "add": [
              [
                "name": "JAVA_HOME",
                "value": "/bin/java"
              ]
            ],
            "remove": [
              "URL"
            ]
          ]
        ])

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, updatedConfig) })

        assertThatResponse()
          .isOk()
          .hasJsonBody(json)
      }

      @Test
      void 'should error out if the environment does not exist'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(eq("env1"), any(HttpLocalizedOperationResult)))
          .then({
          InvocationOnMock invocation ->
            def result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.badRequest("The environment does not exist")
        })
        patchWithApiHeader(controller.controllerPath("env1"), [name: "env1"])

        assertThatResponse()
          .isBadRequest()
          .hasJsonMessage("The environment does not exist")
      }
    }
  }


  @Nested
  class Create {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'create'
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), [])
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }


      @Test
      void 'should create environment with specified parameters'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        def json = toObjectString({ EnvironmentRepresenter.toJSON(it, env1) })

        when(environmentConfigService.createEnvironment(eq(env1), eq(currentUsername()), any(HttpLocalizedOperationResult))).then({
          InvocationOnMock invocation ->
            HttpLocalizedOperationResult result = (HttpLocalizedOperationResult) invocation.arguments.last()
            result.setMessage("Environment was created successfully")
        })

        postWithApiHeader(controller.controllerPath(), json)

        assertThatResponse()
          .isOk()
          .hasJsonBody(json)
      }

      @Test
      void 'should error out if the environment already exists'() {
        when(environmentConfigService.getMergedEnvironmentforDisplay(anyString(), any(HttpLocalizedOperationResult.class)))
          .thenReturn(new ConfigElementForEdit<EnvironmentConfig>(new BasicEnvironmentConfig(), "md5string"))

        postWithApiHeader(controller.controllerPath(), [name: "env1"])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Failed to add environment 'env1'. Another environment with the same name already exists.")
      }
    }
  }


  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'index'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should return all configured environments'() {
        def env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
        env1.addAgent("agent1")
        env1.addAgent("agent2")
        env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
        env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
        env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

        def env2 = new BasicEnvironmentConfig(new CaseInsensitiveString("env2"))

        def listOfEnvironmentConfigs = [env1, env2]

        when(environmentConfigService.getAllMergedEnvironments()).thenReturn(listOfEnvironmentConfigs)

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse().hasJsonBody([
          "_embedded": [
            "environments": [
              [
                "_links"               : [
                  "doc" : [
                    "href": "https://api.go.cd/current/#environment-config"
                  ],
                  "find": [
                    "href": "http://test.host/go/api/admin/environments/:name"
                  ],
                  "self": [
                    "href": "http://test.host/go/api/admin/environments/env1"
                  ]
                ],
                "agents"               : [
                  [
                    "_links": [
                      "doc" : [
                        "href": "https://api.gocd.org/current/#agents"
                      ],
                      "find": [
                        "href": "/api/agents/:uuid"
                      ],
                      "self": [
                        "href": "http://test.host/go/api/agents/agent1"
                      ]
                    ],
                    "uuid"  : "agent1"
                  ],
                  [
                    "_links": [
                      "doc" : [
                        "href": "https://api.gocd.org/current/#agents"
                      ],
                      "find": [
                        "href": "/api/agents/:uuid"
                      ],
                      "self": [
                        "href": "http://test.host/go/api/agents/agent2"
                      ]
                    ],
                    "uuid"  : "agent2"
                  ]
                ],
                "environment_variables": [
                  [
                    "name"  : "JAVA_HOME",
                    "secure": false,
                    "value" : "/bin/java"
                  ]
                ],
                "name"                 : "env1",
                "pipelines"            : [
                  [
                    "_links": [
                      "doc" : [
                        "href": "https://api.go.cd/current/#pipelines"
                      ],
                      "find": [
                        "href": "/api/admin/pipelines/:pipeline_name"
                      ],
                      "self": [
                        "href": "http://test.host/go/api/pipelines/Pipeline1/history"
                      ]
                    ],
                    "name"  : "Pipeline1"
                  ],
                  [
                    "_links": [
                      "doc" : [
                        "href": "https://api.go.cd/current/#pipelines"
                      ],
                      "find": [
                        "href": "/api/admin/pipelines/:pipeline_name"
                      ],
                      "self": [
                        "href": "http://test.host/go/api/pipelines/Pipeline2/history"
                      ]
                    ],
                    "name"  : "Pipeline2"
                  ]
                ]
              ],
              [
                "_links"               : [
                  "doc" : [
                    "href": "https://api.go.cd/current/#environment-config"
                  ],
                  "find": [
                    "href": "http://test.host/go/api/admin/environments/:name"
                  ],
                  "self": [
                    "href": "http://test.host/go/api/admin/environments/env2"
                  ]
                ],
                "agents"               : [],
                "environment_variables": [],
                "name"                 : "env2",
                "pipelines"            : []
              ]
            ]
          ],
          "_links"   : [
            "doc" : [
              "href": "https://api.go.cd/current/#environment-config"
            ],
            "find": [
              "href": "http://test.host/go/api/admin/environments/:name"
            ],
            "self": [
              "href": "http://test.host/go/api/admin/environments"
            ]
          ]
        ])
      }


      @Test
      void 'should return empty environments when no environements are configured'() {
        def expectedResponse = [
          "_embedded": [
            "environments": []
          ],
          "_links"   : [
            "doc" : [
              "href": "https://api.go.cd/current/#environment-config"
            ],
            "find": [
              "href": "http://test.host/go/api/admin/environments/:name"
            ],
            "self": [
              "href": "http://test.host/go/api/admin/environments"
            ]
          ]
        ]

        when(environmentConfigService.getAllMergedEnvironments()).thenReturn([])

        getWithApiHeader(controller.controllerBasePath())

        assertThatResponse().hasJsonBody(expectedResponse)
      }
    }
  }
}
