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
package com.thoughtworks.go.apiv6.plugininfos.representers


import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helpers.PluginInfoMother.*
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PluginInfoRepresenterTest {

  static LinkedHashMap<Object, Object> expectedJson() {
    return [
      "_links"              : [
        "doc"  : [
          "href": apiDocsUrl("#plugin-info")
        ],
        "find" : [
          "href": "http://test.host/go/api/admin/plugin_info/:id"
        ],
        "image": [
          "href": "http://test.host/go/api/plugin_images/plugin_id/hash"
        ],
        "self" : [
          "href": "http://test.host/go/api/admin/plugin_info/plugin_id"
        ]
      ],
      "id"                  : "plugin_id",
      "status"              : [
        "state": "active"
      ],
      "plugin_file_location": "/home/pluginjar/",
      "bundled_plugin"      : true,
      "about"               : [
        "name"                    : "GoPlugin",
        "version"                 : "v1",
        "target_go_version"       : "goVersion1",
        "description"             : "go plugin",
        "target_operating_systems": ["os"],
        "vendor"                  : [
          "name": "go",
          "url" : "goUrl"
        ],
      ],
      "extensions"          : [
        [
          "type"                : "authorization",
          "auth_config_settings": [
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
              ]
            ],
            "view"          : [
              "template": "Template"
            ]
          ],
          "role_settings"       : [
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
              ]
            ],
            "view"          : [
              "template": "Template"
            ]
          ],
          "capabilities"        : [
            "can_search"         : true,
            "supported_auth_type": "Password",
            "can_authorize"      : true
          ]
        ]
      ]
    ]
  }

  @Test
  void 'it should serialize valid pluginInfo'() {
    def actualJson = toObjectString({
      PluginInfoRepresenter.toJSON(it, new CombinedPluginInfo(createAuthorizationPluginInfo()))
    })

    assertThatJson(actualJson).isEqualTo(expectedJson())
  }

  @Test
  void "it should serialize bad plugin info"() {
    def actualJson = toObjectString({
      PluginInfoRepresenter.toJSON(it, new CombinedPluginInfo(createBadPluginInfo()))
    })

    def expectedJson = [
      "_links"              : [
        "doc" : [
          "href": apiDocsUrl("#plugin-info")
        ],
        "find": [
          "href": "http://test.host/go/api/admin/plugin_info/:id"
        ],
        "self": [
          "href": "http://test.host/go/api/admin/plugin_info/bad_plugin"
        ]
      ],
      "id"                  : "bad_plugin",
      "status"              : [
        "state"   : "invalid",
        "messages": [
          "This is bad plugin"
        ]
      ],
      "plugin_file_location": "/home/bad_plugin/plugin_jar/",
      "bundled_plugin"      : true,
      "about"               : [
        "name"                    : "BadPlugin",
        "version"                 : "v1",
        "target_go_version"       : "goVersion1",
        "description"             : "go plugin",
        "target_operating_systems": [],
        "vendor"                  : [
          "name": "go",
          "url" : "goUrl"
        ],
      ],
      "extensions"          : []
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void "should serialize plugin info if about information is not specified"() {
    def actualJson = toObjectString({
      PluginInfoRepresenter.toJSON(it, new CombinedPluginInfo(createAuthorizationPluginInfoWithoutAbout()))
    })
    def expectedJson = [
      "_links"              : [
        "doc"  : [
          "href": apiDocsUrl("#plugin-info")
        ],
        "find" : [
          "href": "http://test.host/go/api/admin/plugin_info/:id"
        ],
        "self" : [
          "href": "http://test.host/go/api/admin/plugin_info/plugin_id"
        ],
        "image": [
          "href": "http://test.host/go/api/plugin_images/plugin_id/hash"
        ]
      ],
      "id"                  : "plugin_id",
      "status"              : [
        "state": "active"
      ],
      "plugin_file_location": "/home/pluginjar/",
      "bundled_plugin"      : true,
      "extensions"          : [
        [
          type                : "authorization",
          auth_config_settings: [
            configurations: [
              [
                key     : "key1",
                metadata: [required: true, secure: false]
              ],
              [
                key     : "key2",
                metadata: [required: true, secure: false]
              ]
            ],
            view          : [template: "Template"]
          ],
          capabilities        : [
            can_authorize      : true,
            can_search         : true,
            supported_auth_type: "Password"
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void "should serialize plugin info if image is not specified"() {
    def actualJson = toObjectString({
      PluginInfoRepresenter.toJSON(it, new CombinedPluginInfo(createAuthorizationPluginInfoWithoutImage()))
    })
    def expectedJson = [
      "_links"              : [
        "doc" : [
          "href": apiDocsUrl("#plugin-info")
        ],
        "find": [
          "href": "http://test.host/go/api/admin/plugin_info/:id"
        ],
        "self": [
          "href": "http://test.host/go/api/admin/plugin_info/plugin_id"
        ]
      ],
      "id"                  : "plugin_id",
      "status"              : [
        "state": "active"
      ],
      "plugin_file_location": "/home/pluginjar/",
      "bundled_plugin"      : true,
      "about"               : [
        "name"                    : "GoPlugin",
        "version"                 : "v1",
        "target_go_version"       : "goVersion1",
        "description"             : "go plugin",
        "target_operating_systems": ["os"],
        "vendor"                  : [
          "name": "go",
          "url" : "goUrl"
        ],
      ],
      "extensions"          : [
        [
          type                : "authorization",
          auth_config_settings: [
            configurations: [
              [
                key     : "key1",
                metadata: [required: true, secure: false]
              ],
              [
                key     : "key2",
                metadata: [required: true, secure: false]
              ]
            ],
            view          : [template: "Template"]
          ],
          capabilities        : [
            can_authorize      : true,
            can_search         : true,
            supported_auth_type: "Password"
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
