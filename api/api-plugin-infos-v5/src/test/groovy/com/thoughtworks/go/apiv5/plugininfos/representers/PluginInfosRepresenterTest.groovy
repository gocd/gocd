package com.thoughtworks.go.apiv5.plugininfos.representers

import com.thoughtworks.go.apiv5.plugininfos.representers.Helper.PluginInfoMother
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PluginInfosRepresenterTest {

  @Test
  void 'should serialize plugin infos to json'() {
    def SCMPluginInfo = PluginInfoMother.createSCMPluginInfo()
    def authorizationPluginInfo = PluginInfoMother.createAuthorizationPluginInfo()

    def SCMCombinedPluginInfo = new CombinedPluginInfo(SCMPluginInfo)
    def authorizationCombinedPluginInfo = new CombinedPluginInfo(authorizationPluginInfo)

    Collection<CombinedPluginInfo> pluginInfos = Arrays.asList(SCMCombinedPluginInfo, authorizationCombinedPluginInfo)

    def actualJson = toObjectString({ PluginInfosRepresenter.toJSON(it, pluginInfos) })

    def expectedJSON = [
      "_links"   : [
        "self": [
          "href": "http://test.host/go/api/admin/plugin_info"
        ],
        "doc" : [
          "href": apiDocsUrl("#plugin-info")
        ],
        "find": [
          "href": "http://test.host/go/api/admin/plugin_info/:id"
        ]
      ],
      "_embedded": [
        "plugin_info": [
          [
            "_links"              : [
              "self": [
                "href": "http://test.host/go/api/admin/plugin_info/plugin_id"
              ],
              "doc" : [
                "href": apiDocsUrl("#plugin-info")
              ],
              "find": [
                "href": "http://test.host/go/api/admin/plugin_info/:id"
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
              ]
            ],
            "extensions"          : [
              [
                "type"        : "scm",
                "display_name": "SCM",
                "scm_settings": [
                  "configurations": [
                    [
                      "key"     : "key1",
                      "metadata": [
                        "secure"          : false,
                        "required"        : true,
                        "part_of_identity": true
                      ]
                    ]
                  ],
                  "view"          : [
                    "template": "Template"
                  ]
                ]
              ]
            ]
          ],
          [
            "_links"              : [
              "self" : [
                "href": "http://test.host/go/api/admin/plugin_info/plugin_id"
              ],
              "doc"  : [
                "href": apiDocsUrl("#plugin-info")
              ],
              "image": [
                "href": "http://test.host/go/api/plugin_images/plugin_id/hash"
              ],
              "find" : [
                "href": "http://test.host/go/api/admin/plugin_info/:id"
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
              ]
            ],
            "extensions"          : [
              [
                "type"                : "authorization",
                "auth_config_settings": [
                  "configurations": [
                    [
                      "key"     : "key1",
                      "metadata": [
                        "secure"  : false,
                        "required": true
                      ]
                    ],
                    [
                      "key"     : "key2",
                      "metadata": [
                        "secure"  : false,
                        "required": true
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
                        "secure"  : false,
                        "required": true
                      ]
                    ],
                    [
                      "key"     : "key2",
                      "metadata": [
                        "secure"  : false,
                        "required": true
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
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJSON)
  }
}
