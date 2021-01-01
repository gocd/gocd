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
package com.thoughtworks.go.apiv3.environments.representers


import com.thoughtworks.go.config.BasicEnvironmentConfig
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.EnvironmentConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class EnvironmentsRepresenterTest {

  @Test
  void 'should serialize list of environments to JSON'() {
    EnvironmentConfig env1 = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"))
    env1.addAgent("agent1")
    env1.addAgent("agent2")
    env1.addEnvironmentVariable("JAVA_HOME", "/bin/java")
    env1.addPipeline(new CaseInsensitiveString("Pipeline1"))
    env1.addPipeline(new CaseInsensitiveString("Pipeline2"))

    EnvironmentConfig env2 = new BasicEnvironmentConfig(new CaseInsensitiveString("env2"))
    env2.addEnvironmentVariable("GROOVY_HOME", "/bin/groovy")
    env2.addPipeline(new CaseInsensitiveString("Pipeline3"))

    def listOfEnvironments = [env1, env2]

    def json = toObjectString({ EnvironmentsRepresenter.toJSON(it, listOfEnvironments) })

    assertThatJson(json).isEqualTo([
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
            "environment_variables": [
              [
                "name"  : "GROOVY_HOME",
                "secure": false,
                "value" : "/bin/groovy"
              ]
            ],
            "name"                 : "env2",
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
                    "href": "http://test.host/go/api/pipelines/Pipeline3/history"
                  ]
                ],
                "name"  : "Pipeline3"
              ]
            ]
          ]
        ]
      ],
      "_links"   : [
        "doc" : [
          "href": "https://api.go.cd/current/#environment-config"
        ],
        "self": [
          "href": "http://test.host/go/api/admin/environments"
        ]
      ]
    ])
  }

  @Test
  void 'should serialize an empty list of environments to JSON'() {
    def json = toObjectString({ EnvironmentsRepresenter.toJSON(it, []) })

    assertThatJson(json).isEqualTo([
      "_embedded": [
        "environments": []
      ],
      "_links"   : [
        "doc" : [
          "href": "https://api.go.cd/current/#environment-config"
        ],
        "self": [
          "href": "http://test.host/go/api/admin/environments"
        ]
      ]
    ])
  }
}
