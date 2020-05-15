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
package com.thoughtworks.go.apiv7.plugininfos.representers.extensions

import com.thoughtworks.go.helpers.PluginInfoMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helpers.PluginInfoMother.createElasticAgentPluginInfoForV4
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ElasticAgentExtensionRepresenterTest {
  @Test
  void 'should serialize Elastic agent info to json for elastic agent V4 extension'() {
    def actualJson = toObjectString({
      new ElasticAgentExtensionRepresenter().toJSON(it, createElasticAgentPluginInfoForV4())
    })
    def expectedJSON = [
      "type"                          : "elastic-agent",
      "plugin_settings"               : [
        "configurations": [
          [
            "key"     : "key1",
            "metadata": [
              "required": true,
              "secure"  : false
            ]
          ],
          [
            "key"     : "key2",
            "metadata": [
              "required": true,
              "secure"  : false
            ]
          ]],
        "view"          : [
          "template": "Template"
        ]
      ],
      "supports_cluster_profiles"     : false,
      "elastic_agent_profile_settings": [
        "configurations": [
          [
            "key"     : "key1",
            "metadata": [
              "required": true, "secure": false
            ]
          ],
          [
            "key"     : "key2",
            "metadata": [
              "required": true,
              "secure"  : false
            ]
          ]],
        "view"          : [
          "template": "Template"
        ]
      ],
      "capabilities"                  : [
        "supports_agent_status_report"  : false,
        "supports_cluster_status_report": false,
        "supports_plugin_status_report" : true
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }

  @Test
  void 'should serialize Elastic agent plugin info to json for elastic agent extension v5'() {
    def actualJson = toObjectString({
      new ElasticAgentExtensionRepresenter().toJSON(it, PluginInfoMother.createElasticAgentPluginInfoForV5())
    })
    def expectedJSON = [
      "type"                          : "elastic-agent",
      "cluster_profile_settings"      : [
        "configurations": [
          [
            "key"     : "key1",
            "metadata": [
              "required": true,
              "secure"  : false
            ]
          ],
          [
            "key"     : "key2",
            "metadata": [
              "required": true,
              "secure"  : false
            ]
          ]],
        "view"          : [
          "template": "Template"
        ]
      ],
      "supports_cluster_profiles"     : true,
      "elastic_agent_profile_settings": [
        "configurations": [
          [
            "key"     : "key1",
            "metadata": [
              "required": true, "secure": false
            ]
          ],
          [
            "key"     : "key2",
            "metadata": [
              "required": true,
              "secure"  : false
            ]
          ]],
        "view"          : [
          "template": "Template"
        ]
      ],
      "capabilities"                  : [
        "supports_agent_status_report"  : true,
        "supports_cluster_status_report": true,
        "supports_plugin_status_report" : true
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
