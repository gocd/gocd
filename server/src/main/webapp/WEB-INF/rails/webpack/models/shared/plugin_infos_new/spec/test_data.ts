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
import {
  AboutJSON,
  AnalyticsCapabilityJSON,
  AnalyticsExtensionJSON,
  AuthorizationExtensionJSON,
  ElasticAgentExtensionJSON,
  LinksJSON,
  PluginInfoJSON,
  SecretConfigExtensionJSON,
  StatusJSON,
  VendorJSON,
  ViewJSON
} from "models/shared/plugin_infos_new/serialization";

export function pluginImageLink() {
  return {
    image: {
      href: "https://example.com/image"
    }
  } as LinksJSON;
}

class BasePluginInfo {
  protected static withExtension(extension: AnalyticsExtensionJSON | AuthorizationExtensionJSON,
                                 pluginId: string = "gocd.analytics.plugin",
                                 name: string     = "Analytics plugin") {
    return {
      _links: pluginImageLink(),
      id: pluginId,
      status: {
        state: "active"
      },
      plugin_file_location: "/foo/bar.jar",
      bundled_plugin: false,
      about: {
        name,
        version: "0.0.1",
        target_go_version: "19.10.0",
        description: "Some description about the plugin",
        target_operating_systems: [],
        vendor: {
          name: "GoCD Contributors",
          url: "https://foo/bar"
        }
      },
      extensions: [extension],
    } as PluginInfoJSON;
  }
}

export const someVendor: VendorJSON = {
  name: "GoCD Contributors",
  url: "https://github.com/gocd-contrib/docker-elastic-agents"
};

export const about: AboutJSON = {
  name: "Docker Elastic Agent Plugin",
  version: "0.6.1",
  target_go_version: "16.12.0",
  description: "Docker Based Elastic Agent Plugins for GoCD",
  target_operating_systems: [
    "Linux",
    "Mac OS X"
  ],
  vendor: someVendor
};

export const activeStatus: StatusJSON = {
  state: "active"
};

export const view: ViewJSON = {
  template: "<div>some view for plugin</div>"
};

export const pluginInfoWithElasticAgentExtensionV5: PluginInfoJSON = {
  _links: pluginImageLink(),
  id: "cd.go.contrib.elastic-agent.docker",
  status: activeStatus,
  plugin_file_location: "/foo/bar.jar",
  bundled_plugin: false,
  about,
  extensions: [
    {
      type: "elastic-agent",
      cluster_profile_settings: {
        configurations: [
          {
            key: "instance_type",
            metadata: {
              secure: false,
              required: true
            }
          }
        ],
        view: {
          template: "elastic agent plugin settings view"
        }
      },
      elastic_agent_profile_settings: {
        configurations: [
          {
            key: "Image",
            metadata: {
              secure: false,
              required: true
            }
          },
          {
            key: "Command",
            metadata: {
              secure: false,
              required: false
            }
          },
          {
            key: "Environment",
            metadata: {
              secure: false,
              required: false
            }
          }
        ],
        view
      },
      capabilities: {
        supports_plugin_status_report: true,
        supports_cluster_status_report: true,
        supports_agent_status_report: true
      },
      supports_cluster_profiles: true
    } as ElasticAgentExtensionJSON
  ]
};

const elasticAgentExtensionJSON: ElasticAgentExtensionJSON = {
  type: "elastic-agent",
  plugin_settings: {
    configurations: [
      {
        key: "instance_type",
        metadata: {
          secure: false,
          required: true
        }
      }
    ],
    view: {
      template: "elastic agent plugin settings view"
    }
  },
  elastic_agent_profile_settings: {
    configurations: [
      {
        key: "Image",
        metadata: {
          secure: false,
          required: true
        }
      },
      {
        key: "Command",
        metadata: {
          secure: false,
          required: false
        }
      },
      {
        key: "Environment",
        metadata: {
          secure: false,
          required: false
        }
      }
    ],
    view
  },
  capabilities: {
    supports_plugin_status_report: true,
    supports_cluster_status_report: true,
    supports_agent_status_report: true
  },
  supports_cluster_profiles: false
};

export const pluginInfoWithElasticAgentExtensionV4: PluginInfoJSON = {
  _links: pluginImageLink(),
  id: "cd.go.contrib.elastic-agent.docker",
  status: activeStatus,
  plugin_file_location: "/foo/bar.jar",
  bundled_plugin: false,
  about,
  extensions: [
    elasticAgentExtensionJSON
  ]
};

export class ArtifactPluginInfo {
  static docker() {
    return {
      _links: pluginImageLink(),
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
    } as PluginInfoJSON;
  }
}

export class AuthorizationPluginInfo extends BasePluginInfo {
  static file() {
    const extension = {
      type: "authorization",
      auth_config_settings: {
        configurations: [
          {
            key: "PasswordFilePath",
            metadata: {
              secure: true,
              required: true
            }
          },
          {
            key: "AnotherProperty",
            metadata: {
              secure: true,
              required: true
            }
          }
        ],
        view: {
          template: "<div class=\"form_item_block\">This is ldap auth config view.</div>"
        }
      },
      capabilities: {
        can_authorize: false,
        can_search: false,
        supported_auth_type: "Password"
      }
    } as AuthorizationExtensionJSON;

    return this.withExtension(extension,
                              "cd.go.authorization.file",
                              "File based authorization plugin");
  }

  static ldap(): PluginInfoJSON {
    return {
      _links: pluginImageLink(),
      id: "cd.go.authorization.ldap",
      status: {
        state: "active"
      },
      plugin_file_location: "/go-working-dir/plugins/external/github-oauth-authorization.jar",
      bundled_plugin: false,
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
              template: `
                          <div class="form_item_block">
                            This is ldap auth config view.
                            <input type="text" id="test-field-1"/>
                            <textarea id="test-field-2"></textarea>
                            <select id="test-field-3"><option value="test">Test</option></select>
                          </div>`
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
        } as AuthorizationExtensionJSON
      ]
    } as PluginInfoJSON;
  }

  static github() {
    return {
      _links: pluginImageLink(),
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
    } as PluginInfoJSON;
  }
}

export class SecretPluginInfo {
  static file() {
    return {
      _links: pluginImageLink(),
      id: "cd.go.secrets.file",
      status: {
        state: "active"
      },
      plugin_file_location: "/go-working-dir/plugins/external/github-oauth-authorization.jar",
      bundled_plugin: false,
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
        } as SecretConfigExtensionJSON
      ]
    } as PluginInfoJSON;
  }
}

export class AnalyticsPluginInfo extends BasePluginInfo {
  static with(pluginId: string, name: string) {
    return this.withExtension(this.analyticsExtension(), pluginId, name);
  }

  static withCapabilities(...capabilities: AnalyticsCapabilityJSON[]) {
    const analyticsExtensionJSON                            = this.analyticsExtension();
    analyticsExtensionJSON.capabilities.supported_analytics = capabilities;
    return this.withExtension(analyticsExtensionJSON);
  }

  static analytics() {
    return this.withExtension(this.analyticsExtension());
  }

  static analyticsExtension() {
    return {
      type: "analytics",
      plugin_settings: {
        configurations: [
          {
            key: "username",
            metadata: {
              secure: false,
              required: true
            }
          }
        ],
        view: {
          template: "analytics plugin view"
        }
      },
      capabilities: {
        supported_analytics: [
          {type: "agent", id: "bar", title: "bar"},
          {type: "pipeline", id: "rawr", title: "foo"},
          {type: "dashboard", id: "foo", title: "something"}
        ]
      }
    } as AnalyticsExtensionJSON;
  }
}

export class TaskPluginInfo {
  static scriptExecutor(): PluginInfoJSON {
    return {
      _links: pluginImageLink(),
      id: "script-executor",
      status: {
        state: "active"
      },
      plugin_file_location: "server/plugins/external/script-executor-0.3.0.jar",
      bundled_plugin: false,
      about: {
        name: "Script Executor",
        version: "0.3.0",
        target_go_version: "16.1.0",
        description: "Thoughtworks Go plugin to run scripts",
        target_operating_systems: [],
        vendor: {
          name: "Srinivas Upadhya",
          url: "https://github.com/srinivasupadhya"
        }
      },
      extensions: [{
        type: "task",
        display_name: "Script Executor",
        task_settings: {
          configurations: [{
            key: "script",
            metadata: {
              secure: false,
              required: true
            }
          }, {
            key: "shtype",
            metadata: {
              secure: false,
              required: true
            }
          }],
          view: {
            template: "this is plugin view"
          }
        }
      }
      ]
    };
  }
}
