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
package com.thoughtworks.go.apiv6.agents.representers

import com.thoughtworks.go.config.EnvironmentConfig
import com.thoughtworks.go.domain.AgentInstance
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.SecurityService
import com.thoughtworks.go.util.SystemUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.AgentInstanceMother.*
import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment
import static java.util.Arrays.asList
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class AgentsRepresenterTest {
  @Mock
  private SecurityService securityService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Test
  void 'should represent agents'() {
    def idle = idle()
    def missing = missing()
    def building = building("up42/1/up_42_stage/1/up42_job")

    idle.getAgent().setEnvironments("uat,load_test")
    missing.getAgent().setEnvironments("unit")
    building.getAgent().setEnvironments("integration,functional_test")

    Map<AgentInstance, Collection<EnvironmentConfig>> agentEnvironmentMap = new LinkedHashMap<AgentInstance, Collection<EnvironmentConfig>>() {
      {
        put(idle, asList(environment("uat"), environment("load_test")))
        put(missing, asList(environment("unit")))
        put(building, asList(environment("integration"), environment("functional_test")))
      }
    }

    when(securityService.hasViewOrOperatePermissionForPipeline(any() as Username, anyString())).thenReturn(true)

    def json = toObjectString({
      AgentsRepresenter.toJSON(it, agentEnvironmentMap, securityService, new Username("bob"))
    })

    def expectedJson = [
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
              [
                name  : "load_test",
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
                name  : "uat",
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
              ]
            ],
            "build_state"       : "Idle"
          ],
          [
            "_links"            : [
              "self": [
                "href": "http://test.host/go/api/agents/1234"
              ],
              "doc" : [
                "href": apiDocsUrl("#agents")
              ],
              "find": [
                "href": "http://test.host/go/api/agents/:uuid"
              ]
            ],
            "uuid"              : "1234",
            "hostname"          : "localhost",
            "ip_address"        : "192.168.0.1",
            "sandbox"           : "",
            "operating_system"  : "",
            "free_space"        : "unknown",
            "agent_config_state": "Enabled",
            "agent_state"       : "Missing",
            "resources"         : [],
            "environments"      : [
              [
                name  : "unit",
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
              ]
            ],
            "build_state"       : "Unknown"
          ],
          [
            "_links"            : [
              "self": [
                "href": "http://test.host/go/api/agents/uuid3"
              ],
              "doc" : [
                "href": apiDocsUrl("#agents")
              ],
              "find": [
                "href": "http://test.host/go/api/agents/:uuid"
              ]
            ],
            "uuid"              : "uuid3",
            "hostname"          : "CCeDev01",
            "ip_address"        : "10.18.5.1",
            "sandbox"           : SystemUtil.currentWorkingDirectory(),
            "operating_system"  : "",
            "free_space"        : "unknown",
            "agent_config_state": "Enabled",
            "agent_state"       : "Building",
            "resources"         : [
              "java"
            ],
            "environments"      : [
              [
                name  : "functional_test",
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
                name  : "integration",
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
              ]
            ],
            "build_state"       : "Building",
            "build_details"     : [
              "_links"       : [
                "job"     : [
                  "href": "http://test.host/go/tab/build/detail/up42/1/up_42_stage/1/up42_job"
                ],
                "stage"   : [
                  "href": "http://test.host/go/pipelines/up42/1/up_42_stage/1"
                ],
                "pipeline": [
                  "href": "http://test.host/go/pipeline/activity/up42"
                ]
              ],
              "pipeline_name": "up42",
              "stage_name"   : "up_42_stage",
              "job_name"     : "up42_job"
            ]
          ]
        ]
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJson)
  }
}
