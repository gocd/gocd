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
import com.thoughtworks.go.config.BasicEnvironmentConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class EnvironmentsControllerV2Test implements SecurityServiceTrait, ControllerTrait<EnvironmentsControllerV2> {

  @Mock
  EnvironmentConfigService environmentConfigService

  @Override
  EnvironmentsControllerV2 createControllerInstance() {
    new EnvironmentsControllerV2(new ApiAuthenticationHelper(securityService, goConfigService), environmentConfigService)
  }

  @BeforeEach
  void setup() {
    initMocks(this)
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
                "_links": [
                  "doc": [
                    "href": "https://api.go.cd/current/#environment-config"
                  ],
                  "find": [
                    "href": "http://test.host/go/api/admin/environments/:name"
                  ],
                  "self": [
                    "href": "http://test.host/go/api/admin/environments/env1"
                  ]
                ],
                "agents": [
                  [
                    "_links": [
                      "doc": [
                        "href": "https://api.gocd.org/current/#agents"
                      ],
                      "find": [
                        "href": "/api/agents/:uuid"
                      ],
                      "self": [
                        "href": "http://test.host/go/api/agents/agent1"
                      ]
                    ],
                    "uuid": "agent1"
                  ],
                  [
                    "_links": [
                      "doc": [
                        "href": "https://api.gocd.org/current/#agents"
                      ],
                      "find": [
                        "href": "/api/agents/:uuid"
                      ],
                      "self": [
                        "href": "http://test.host/go/api/agents/agent2"
                      ]
                    ],
                    "uuid": "agent2"
                  ]
                ],
                "environment_variables": [
                  [
                    "name": "JAVA_HOME",
                    "secure": false,
                    "value": "/bin/java"
                  ]
                ],
                "name": "env1",
                "pipelines": [
                  [
                    "_links": [
                      "doc": [
                        "href": "https://api.go.cd/current/#pipelines"
                      ],
                      "find": [
                        "href": "/api/admin/pipelines/:pipeline_name"
                      ],
                      "self": [
                        "href": "http://test.host/go/api/pipelines/Pipeline1/history"
                      ]
                    ],
                    "name": "Pipeline1"
                  ],
                  [
                    "_links": [
                      "doc": [
                        "href": "https://api.go.cd/current/#pipelines"
                      ],
                      "find": [
                        "href": "/api/admin/pipelines/:pipeline_name"
                      ],
                      "self": [
                        "href": "http://test.host/go/api/pipelines/Pipeline2/history"
                      ]
                    ],
                    "name": "Pipeline2"
                  ]
                ]
              ],
              [
                "_links": [
                  "doc": [
                    "href": "https://api.go.cd/current/#environment-config"
                  ],
                  "find": [
                    "href": "http://test.host/go/api/admin/environments/:name"
                  ],
                  "self": [
                    "href": "http://test.host/go/api/admin/environments/env2"
                  ]
                ],
                "agents": [],
                "environment_variables": [],
                "name": "env2",
                "pipelines": []
              ]
            ]
          ],
          "_links": [
            "doc": [
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
          "_links": [
            "doc": [
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
