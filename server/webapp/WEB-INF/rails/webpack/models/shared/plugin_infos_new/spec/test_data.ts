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

export class PluginInfoTestData {
  static list(...objects: any[]) {
    return {
      _links: {
        self: {
          href: "https://ci.example.com/go/api/admin/plugin_info"
        },
        find: {
          href: "https://ci.example.com/go/api/admin/plugin_info/:plugin_id"
        },
        doc: {
          href: "https://api.gocd.org/#plugin-info"
        }
      },
      _embedded: {
        plugin_info: objects
      }
    };
  }

  static authorization() {
    return {
      id: "cd.go.authorization.ldap",
      status: {
        state: "active"
      },
      about: {
        name: "LDAP Authorization Plugin for GoCD",
        version: "0.0.1",
        target_go_version: "16.12.0",
        description: "LDAP Authorization Plugin for GoCD",
        target_operating_systems: [],
        vendor: {
          name: "ThoughtWorks, Inc. & GoCD Contributors",
          url: "https://github.com/gocd/gocd-ldap-authorization-plugin"
        }
      },
      extensions: [
        {
          type: "authorization",
          auth_config_settings: {
            configurations: [
              {
                key: "Url",
                metadata: {
                  secure: false,
                  required: true
                }
              },
              {
                key: "SearchBases",
                metadata: {
                  secure: false,
                  required: true
                }
              },
              {
                key: "ManagerDN",
                metadata: {
                  secure: false,
                  required: true
                }
              },
              {
                key: "Password",
                metadata: {
                  secure: true,
                  required: true
                }
              },
            ],
            view: {
              template: "<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Url].$error.server}\">URI:<span class=\"asterix\">*</span></label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Url].$error.server}\" type=\"text\" ng-model=\"Url\" ng-required=\"true\" placeholder=\"ldap://your.first.uri:port\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Url].$error.server}\" ng-show=\"GOINPUTNAME[Url].$error.server\">{{GOINPUTNAME[Url].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[SearchBases].$error.server}\">Search Base:<span class=\"asterix\">*</span></label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[SearchBases].$error.server}\" type=\"text\" ng-model=\"SearchBases\" ng-required=\"true\" rows=\"3\" placeholder=\"\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[SearchBases].$error.server}\" ng-show=\"GOINPUTNAME[SearchBases].$error.server\">{{GOINPUTNAME[SearchBases].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[ManagerDN].$error.server}\">Manager DN:<span class=\"asterix\">*</span></label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[ManagerDN].$error.server}\" type=\"text\" ng-model=\"ManagerDN\" ng-required=\"true\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[ManagerDN].$error.server}\" ng-show=\"GOINPUTNAME[ManagerDN].$error.server\">{{GOINPUTNAME[ManagerDN].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[Password].$error.server}\">Password:<span class=\"asterix\">*</span></label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[Password].$error.server}\" type=\"password\" ng-model=\"Password\" ng-required=\"true\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[Password].$error.server}\" ng-show=\"GOINPUTNAME[Password].$error.server\">{{GOINPUTNAME[Password].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n  <label ng-class=\"{'is-invalid-label': GOINPUTNAME[LoginAttribute].$error.server}\">Search Filter:<span class=\"asterix\">*</span></label>\n  <input ng-class=\"{'is-invalid-input': GOINPUTNAME[LoginAttribute].$error.server}\" type=\"text\" ng-model=\"LoginAttribute\" ng-required=\"true\"/>\n  <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[LoginAttribute].$error.server}\" ng-show=\"GOINPUTNAME[LoginAttribute].$error.server\">{{GOINPUTNAME[LoginAttribute].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[SearchAttributes].$error.server}\">Search Attributes:</label>\n    <input ng-class=\"{'is-invalid-input': GOINPUTNAME[SearchAttributes].$error.server}\" type=\"text\" ng-model=\"SearchAttributes\" ng-required=\"true\" placeholder=\"uid,cn,mail\"/>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[SearchAttributes].$error.server}\" ng-show=\"GOINPUTNAME[SearchAttributes].$error.server\">{{GOINPUTNAME[SearchAttributes].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n  <label ng-class=\"{'is-invalid-label': GOINPUTNAME[DisplayNameAttribute].$error.server}\">Display Name Attribute:<span class=\"asterix\">*</span></label>\n  <input ng-class=\"{'is-invalid-input': GOINPUTNAME[DisplayNameAttribute].$error.server}\" type=\"text\" ng-model=\"DisplayNameAttribute\" ng-required=\"true\"/>\n  <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[DisplayNameAttribute].$error.server}\" ng-show=\"GOINPUTNAME[DisplayNameAttribute].$error.server\">{{GOINPUTNAME[DisplayNameAttribute].$error.server}}</span>\n</div>\n\n<div class=\"form_item_block\">\n  <label ng-class=\"{'is-invalid-label': GOINPUTNAME[EmailAttribute].$error.server}\">Email Attribute:<span class=\"asterix\">*</span></label>\n  <input ng-class=\"{'is-invalid-input': GOINPUTNAME[EmailAttribute].$error.server}\" type=\"text\" ng-model=\"EmailAttribute\" ng-required=\"true\"/>\n  <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[EmailAttribute].$error.server}\" ng-show=\"GOINPUTNAME[EmailAttribute].$error.server\">{{GOINPUTNAME[EmailAttribute].$error.server}}</span>\n</div>\n\n\n\n"
            }
          },
          role_settings: {
            configurations: [
              {
                key: "AttributeName",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "AttributeValue",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "GroupMembershipFilter",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "GroupMembershipSearchBase",
                metadata: {
                  secure: false,
                  required: false
                }
              }
            ],
            view: {
              template: "<div class=\"row\">\n  <h4>Query user attribute</h4>\n  <div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[AttributeName].$error.server}\">Attribute name</label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[AttributeName].$error.server}\" type=\"text\" ng-model=\"AttributeName\" ng-required=\"true\" placeholder=\"OU=foo,dc=example,dc=com\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[AttributeName].$error.server}\" ng-show=\"GOINPUTNAME[AttributeName].$error.server\">{{GOINPUTNAME[AttributeName].$error.server}}</span>\n  </div>\n\n  <div class=\"form_item_block\">\n    <label ng-class=\"{'is-invalid-label': GOINPUTNAME[AttributeValue].$error.server}\">Attribute value</label>\n    <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[AttributeValue].$error.server}\" type=\"text\" ng-model=\"AttributeValue\" ng-required=\"true\" placeholder=\"OU=foo,dc=example,dc=com\"></textarea>\n    <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[AttributeValue].$error.server}\" ng-show=\"GOINPUTNAME[AttributeValue].$error.server\">{{GOINPUTNAME[AttributeValue].$error.server}}</span>\n  </div>\n</div>\n\n<div>\n  <h4>Use group membership filter</h4>\n  <div class=\"row\">\n    <div class=\"form_item_block\">\n      <label ng-class=\"{'is-invalid-label': GOINPUTNAME[GroupMembershipSearchBase].$error.server}\">Group membership search base</label>\n      <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[GroupMembershipSearchBase].$error.server}\" type=\"text\" ng-model=\"GroupMembershipSearchBase\" ng-required=\"true\" placeholder=\"OU=foo,dc=example,dc=com\"></textarea>\n      <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[GroupMembershipSearchBase].$error.server}\" ng-show=\"GOINPUTNAME[GroupMembershipSearchBase].$error.server\">{{GOINPUTNAME[GroupMembershipSearchBase].$error.server}}</span>\n    </div>\n  </div>\n\n  <div class=\"row\">\n    <div class=\"form_item_block\">\n      <label ng-class=\"{'is-invalid-label': GOINPUTNAME[GroupMembershipFilter].$error.server}\">Group membership filter</label>\n      <textarea ng-class=\"{'is-invalid-input': GOINPUTNAME[GroupMembershipFilter].$error.server}\" type=\"text\" ng-model=\"GroupMembershipFilter\" ng-required=\"true\" placeholder=\"OU=foo,dc=example,dc=com\"></textarea>\n      <span class=\"form_error form-error\" ng-class=\"{'is-visible': GOINPUTNAME[GroupMembershipFilter].$error.server}\" ng-show=\"GOINPUTNAME[GroupMembershipFilter].$error.server\">{{GOINPUTNAME[GroupMembershipFilter].$error.server}}</span>\n    </div>\n  </div>\n</div>\n\n\n\n"
            }
          },
          capabilities: {
            can_authorize: true,
            can_search: false,
            supported_auth_type: "web"
          }
        }
      ]
    };
  }

  static artifact() {
    return {
      id: "cd.go.artifact.docker.registry",
      status: {
        state: "active"
      },
      plugin_file_location: "/Volumes/Data/Projects/go/gocd/server/plugins/external/docker-registry-artifact-plugin-1.0.0-3.jar",
      bundled_plugin: false,
      about: {
        name: "Artifact plugin for docker",
        version: "1.0.0-3",
        target_go_version: "18.7.0",
        description: "Plugin allows to push/pull docker image from public or private docker registry",
        target_operating_systems: [],
        vendor: {
          name: "GoCD Contributors",
          url: "https://github.com/gocd/docker-artifact-plugin"
        }
      },
      extensions: [
        {
          type: "artifact",
          capabilities: {},
          store_config_settings: {
            configurations: [
              {
                key: "RegistryURL",
                metadata: {
                  secure: false,
                  required: true
                }
              },
              {
                key: "Username",
                metadata: {
                  secure: false,
                  required: true
                }
              },
              {
                key: "Password",
                metadata: {
                  secure: true,
                  required: true
                }
              }
            ],
            view: {
              template: "<div>This is store config view.</div>"
            }
          },
          artifact_config_settings: {
            configurations: [
              {
                key: "BuildFile",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "Image",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "Tag",
                metadata: {
                  secure: false,
                  required: false
                }
              }
            ],
            view: {
              template: "<div>This is artifact config view.</div>"
            }
          },
          fetch_artifact_settings: {
            configurations: [],
            view: {
              template: "<div>This is fetch view.</div>"
            }
          }
        }
      ]
    };
  }
}
