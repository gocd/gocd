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
package com.thoughtworks.go.apiv3.environments.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.BasicEnvironmentConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.EnvironmentConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class EnvironmentRepresenterTest {

  @Test
  void 'should de-serialize from JSON'() {
    def environmentConfig = [
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
              "href": apiDocsUrl("#agents")
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
              "href": apiDocsUrl("#agents")
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
    ]
    def jsonReader = GsonTransformer.instance.jsonReaderFrom(environmentConfig)
    def object = EnvironmentRepresenter.fromJSON(jsonReader)

    EnvironmentConfig expectedObject = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
    expectedObject.addEnvironmentVariable("JAVA_HOME", "/bin/java")
    expectedObject.addPipeline(new CaseInsensitiveString("Pipeline1"))
    expectedObject.addPipeline(new CaseInsensitiveString("Pipeline2"))


    assertThat(object).isEqualTo(expectedObject)
  }

  @Test
  void 'should serialize to JSON'() {
    EnvironmentConfig env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
    env1.addAgent("agent1")
    env1.addAgent("agent2")
    env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
    env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
    env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

    def json = toObjectString({ EnvironmentRepresenter.toJSON(it, env1) })

    def response = [
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
    ]

    assertThatJson(json).isEqualTo(response)
  }
}
