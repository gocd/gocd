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
}

export class ArtifactPluginInfo {
  static docker() {
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

export class AuthorizationPluginInfo {
  static ldap() {
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
              template: "<div class=\"form_item_block\">This is ldap auth config view.</div>"
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
              template: "<div class=\"row\">This is ldap role config view.</div>\n\n\n\n"
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

  static github() {
    return {
      id: "cd.go.authorization.github",
      status: {
        state: "active"
      },
      plugin_file_location: "/go-working-dir/plugins/external/github-oauth-authorization.jar",
      bundled_plugin: false,
      about: {
        name: "GitHub OAuth authorization plugin",
        version: "2.2.0-21",
        target_go_version: "17.5.0",
        description: "GitHub OAuth authorization plugin for GoCD",
        target_operating_systems: [],
        vendor: {
          name: "GoCD Contributors",
          url: "https://github.com/gocd-contrib/github-oauth-authorization-plugin"
        }
      },
      extensions: [
        {
          type: "authorization",
          auth_config_settings: {
            configurations: [
              {
                key: "ClientId",
                metadata: {
                  secure: true,
                  required: true
                }
              },
              {
                key: "ClientSecret",
                metadata: {
                  secure: true,
                  required: true
                }
              }
            ],
            view: {
              template: "<div data-plugin-style-id=\"oauth-authorization-plugin\">This is github auth config view.</div>"
            }
          },
          role_settings: {
            configurations: [
              {
                key: "Organizations",
                metadata: {
                  secure: false,
                  required: false
                }
              },
              {
                key: "Teams",
                metadata: {
                  secure: false,
                  required: false
                }
              }
            ],
            view: {
              template: "<div class=\"form_item_block\">This is github role config view.</div>"
            }
          },
          capabilities: {
            can_search: true,
            supported_auth_type: "Web",
            can_authorize: true
          }
        }
      ]
    };
  }
}

export class SecretPluginInfo {
  static file() {
    return {
      id: "cd.go.secrets.file",
      status: {
        state: "active"
      },
      about: {
        name: "File based secrets plugin",
        version: "0.0.1",
        target_go_version: "19.3.0",
        description: "Some description about the plugin",
        target_operating_systems: [],
        vendor: {
          name: "GoCD Contributors",
          url: "https://foo/bar"
        }
      },
      extensions: [
        {
          type: "secrets",
          secret_config_settings: {
            configurations: [
              {
                key: "Url",
                metadata: {
                  secure: false,
                  required: true
                }
              },
              {
                key: "Token",
                metadata: {
                  secure: true,
                  required: true
                }
              }
            ],
            view: {
              template: "<div class=\"form_item_block\">This is secret config view.</div>"
            }
          }
        }
      ]
    };
  }
}
