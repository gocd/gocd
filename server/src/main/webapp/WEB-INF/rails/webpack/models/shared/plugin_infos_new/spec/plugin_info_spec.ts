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
import _ from "lodash";
import {
  AnalyticsExtension,
  ArtifactExtension,
  AuthorizationExtension, ConfigRepoExtension,
  ElasticAgentExtension,
  PackageRepoExtension,
  ScmExtension,
  SecretExtension,
  TaskExtension
} from "models/shared/plugin_infos_new/extensions";
import {
  AboutJSON,
  AnalyticsExtensionJSON,
  ArtifactExtensionJSON,
  AuthorizationExtensionJSON,
  ConfigRepoExtensionJSON,
  ElasticAgentExtensionJSON,
  LinksJSON,
  NotificationExtensionJSON,
  PackageMetadataJSON,
  PackageRepoExtensionJSON,
  PluginInfoJSON,
  SCMExtensionJSON, SCMMetadataJSON,
  SecretConfigExtensionJSON,
  TaskExtensionJSON,
  VendorJSON,
} from "models/shared/plugin_infos_new/serialization";
import {AnalyticsCapability} from "../analytics_plugin_capabilities";
import {ExtensionTypeString} from "../extension_type";
import {PluginInfo} from "../plugin_info";
import {
  about,
  activeStatus, AnalyticsPluginInfo,
  pluginImageLink, pluginInfoWithElasticAgentExtensionV4,
  pluginInfoWithElasticAgentExtensionV5,
  SecretPluginInfo,
  view
} from "./test_data";

describe("PluginInfos New", () => {

  const notificationExtension: NotificationExtensionJSON = {
    type: "notification",
    plugin_settings: {
      configurations: [
        {
          key: "hostname",
          metadata: {
            secure: false,
            required: true
          }
        }
      ],
      view: {
        template: "notification plugin view"
      }
    }
  };

  const pluginInfoWithNotificationExtension: PluginInfoJSON = {
    _links: pluginImageLink(),
    id: "github.pr.status",
    status: activeStatus,
    plugin_file_location: "/foo/bar.jar",
    bundled_plugin: false,
    about,
    extensions: [
      notificationExtension
    ]
  };

  const pluginInfoWithPackageRepositoryExtension: PluginInfoJSON = {
    _links: pluginImageLink(),
    id: "nuget",
    status: activeStatus,
    plugin_file_location: "/foo/bar.jar",
    bundled_plugin: false,
    about,
    extensions: [
      {
        type: "package-repository",
        package_settings: {
          configurations: [
            {
              key: "PACKAGE_ID",
              metadata: {
                part_of_identity: true,
                display_order: 0,
                secure: false,
                display_name: "Package ID",
                required: true
              }
            },
            {
              key: "POLL_VERSION_FROM",
              metadata: {
                part_of_identity: false,
                display_order: 1,
                secure: false,
                display_name: "Version to poll >=",
                required: false
              }
            },
            {
              key: "POLL_VERSION_TO",
              metadata: {
                part_of_identity: false,
                display_order: 2,
                secure: false,
                display_name: "Version to poll <",
                required: false
              }
            },
            {
              key: "INCLUDE_PRE_RELEASE",
              metadata: {
                part_of_identity: false,
                display_order: 3,
                secure: false,
                display_name: "Include Prerelease? (yes/no, defaults to yes)",
                required: false
              }
            }
          ]
        },
        repository_settings: {
          configurations: [
            {
              key: "REPO_URL",
              metadata: {
                part_of_identity: true,
                display_order: 0,
                secure: false,
                display_name: "Repository Url",
                required: true
              }
            },
            {
              key: "USERNAME",
              metadata: {
                part_of_identity: false,
                display_order: 1,
                secure: false,
                display_name: "Username",
                required: false
              }
            },
            {
              key: "PASSWORD",
              metadata: {
                part_of_identity: false,
                display_order: 2,
                secure: true,
                display_name: "Password (use only with https)",
                required: false
              }
            }
          ]
        },
        plugin_settings: {
          configurations: [
            {
              key: "another-property",
              metadata: {
                secure: false,
                required: true
              }
            }
          ],
          view: {
            template: "Plugin Settings View for package repository plugin"
          }
        }
      } as PackageRepoExtensionJSON
    ]
  };

  const pluginInfoWithTaskExtension: PluginInfoJSON = {
    _links: pluginImageLink(),
    id: "docker-task",
    status: activeStatus,
    plugin_file_location: "/foo/bar.jar",
    bundled_plugin: false,
    about,
    extensions: [
      {
        type: "task",
        display_name: "Docker Task",
        task_settings: {
          configurations: [
            {
              key: "DockerFile",
              metadata: {
                secure: false,
                required: false
              }
            },
            {
              key: "DockerRunArguments",
              metadata: {
                secure: false,
                required: false
              }
            },
            {
              key: "IsDockerPush",
              metadata: {
                secure: false,
                required: true
              }
            },
            {
              key: "DockerBuildTag",
              metadata: {
                secure: false,
                required: false
              }
            }
          ],
          view
        }
      } as TaskExtensionJSON
    ]
  };

  const scmExtension: SCMExtensionJSON = {
    type: "scm",
    display_name: "GitHub",
    scm_settings: {
      configurations: [
        {
          key: "url",
          metadata: {
            part_of_identity: true,
            secure: false,
            required: true
          }
        }
      ],
      view
    },
    plugin_settings: {
      configurations: [
        {
          key: "another-property",
          metadata: {
            secure: false,
            required: true
          }
        }
      ],
      view
    }
  };

  const pluginInfoWithSCMExtension: PluginInfoJSON = {
    _links: pluginImageLink(),
    id: "github.pr",
    status: activeStatus,
    plugin_file_location: "/foo/bar.jar",
    bundled_plugin: false,
    about,
    extensions: [
      scmExtension
    ]
  };

  const authorizationExtension: AuthorizationExtensionJSON   = {
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
      view
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
      view
    },
    capabilities: {
      can_authorize: true,
      can_search: false,
      supported_auth_type: "web"
    }
  };
  const pluginInfoWithAuthorizationExtension: PluginInfoJSON = {
    _links: pluginImageLink(),
    id: "cd.go.authorization.ldap",
    status: {
      state: "active"
    },
    plugin_file_location: "/foo/bar.jar",
    bundled_plugin: false,
    about,
    extensions: [
      authorizationExtension
    ]
  };

  const artifactExtension: ArtifactExtensionJSON        = {
    type: "artifact",
    store_config_settings: {
      configurations: [
        {
          key: "S3_BUCKET",
          metadata: {
            secure: false,
            required: true
          }
        },
        {
          key: "AWS_ACCESS_KEY_ID",
          metadata: {
            secure: true,
            required: true
          }
        },
        {
          key: "AWS_SECRET_ACCESS_KEY",
          metadata: {
            secure: true,
            required: true
          }
        }
      ],
      view
    },
    artifact_config_settings: {
      configurations: [
        {
          key: "Filename",
          metadata: {
            secure: false,
            required: false
          }
        }
      ],
      view
    },
    fetch_artifact_settings: {
      configurations: [
        {
          key: "Destination",
          metadata: {
            secure: false,
            required: false
          }
        }
      ],
      view
    }
  };
  const pluginInfoWithArtifactExtension: PluginInfoJSON = {
    _links: pluginImageLink(),
    id: "cd.go.artifact.s3",
    status: {
      state: "active"
    },
    plugin_file_location: "/foo/bar.jar",
    bundled_plugin: false,
    about,
    extensions: [
      artifactExtension
    ]
  };

  const configRepoExtension: ConfigRepoExtensionJSON = {
    type: "configrepo",
    plugin_settings: {
      configurations: [
        {
          key: "pipeline_pattern",
          metadata: {
            required: false,
            secure: false
          }
        }
      ],
      view: {
        template: "config repo plugin view"
      }
    },
    capabilities: {
      supports_parse_content: false,
      supports_pipeline_export: false
    }
  };

  const pluginInfoWithConfigRepoExtension: PluginInfoJSON = {
    _links: pluginImageLink(),
    id: "json.config.plugin",
    status: activeStatus,
    plugin_file_location: "/foo/bar.jar",
    bundled_plugin: false,
    about,
    extensions: [
      configRepoExtension
    ]
  };

  it("should check if plugin settings is supported", () => {
    const withoutPluginSettingsProperty: PluginInfoJSON = {
      _links: pluginImageLink(),
      id: "github.pr",
      status: {
        state: "active"
      },
      plugin_file_location: "/foo/bar.jar",
      bundled_plugin: false,
      about,
      extensions: [
        {
          type: "scm",
          display_name: "Github",
          scm_settings: {
            configurations: [],
            view: {
              template: "<div></div>"
            }
          }
        }
      ]
    };

    const withoutExtensionInfo: PluginInfoJSON = {
      _links: pluginImageLink(),
      id: "github.pr",
      status: {
        state: "active"
      },
      plugin_file_location: "/foo/bar.jar",
      bundled_plugin: false,
      about,
      extensions: []
    };

    const scmExtensionWithoutPluginSettings: SCMExtensionJSON = {
      type: "scm",
      display_name: "foo",
      scm_settings: {
        view,
        configurations: []
      },
    };

    const withoutPluginSettingsView: PluginInfoJSON = {
      _links: pluginImageLink(),
      id: "github.pr",
      status: {
        state: "active"
      },
      plugin_file_location: "/foo/bar.jar",
      bundled_plugin: false,
      about,
      extensions: [
        scmExtensionWithoutPluginSettings
      ]
    };

    const scmExtensionWithoutScmSettings: SCMExtensionJSON = {
      type: "scm",
      plugin_settings: {
        view: {
          template: "plugin settings view"
        }
      }
    } as SCMExtensionJSON;

    const withoutSCMSettingsConfiguration: PluginInfoJSON = {
      _links: pluginImageLink(),
      id: "github.pr",
      status: {
        state: "active"
      },
      plugin_file_location: "/foo/bar.jar",
      bundled_plugin: false,
      about,
      extensions: [
        scmExtensionWithoutScmSettings
      ]
    };

    const pluginInfoWithoutPluginSettings = PluginInfo.fromJSON(withoutPluginSettingsProperty);
    expect(pluginInfoWithoutPluginSettings.supportsPluginSettings()).toBe(false);

    const pluginInfoWithoutExtensionInfo = PluginInfo.fromJSON(withoutExtensionInfo);
    expect(pluginInfoWithoutExtensionInfo.supportsPluginSettings()).toBe(false);

    const pluginInfoWithoutPluginSettingsView = PluginInfo.fromJSON(withoutPluginSettingsView);
    expect(pluginInfoWithoutPluginSettingsView.supportsPluginSettings()).toBe(false);

    const pluginInfoWithoutPluginSettingsConfiguration = PluginInfo.fromJSON(withoutSCMSettingsConfiguration);
    expect(pluginInfoWithoutPluginSettingsConfiguration.supportsPluginSettings()).toBe(false);
  });

  describe("ElasticAgent", () => {
    it("should deserialize plugin info of v4 extension", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithElasticAgentExtensionV4);
      verifyBasicProperties(pluginInfo, pluginInfoWithElasticAgentExtensionV4);

      const extensionInfo = pluginInfo.extensionOfType(ExtensionTypeString.ELASTIC_AGENTS)! as ElasticAgentExtension;
      expect(extensionInfo.profileSettings.viewTemplate())
        .toEqual((pluginInfoWithElasticAgentExtensionV4.extensions[0] as ElasticAgentExtensionJSON).elastic_agent_profile_settings.view!.template);
      expect(extensionInfo.profileSettings.configurations().length).toEqual(3);
      expect(extensionInfo.profileSettings.configurations().map((config) => config.key))
        .toEqual(["Image", "Command", "Environment"]);
      expect(extensionInfo.profileSettings.configurations()[0].metadata).toEqual({
                                                                                   secure: false,
                                                                                   required: true
                                                                                 });

      expect(extensionInfo.supportsClusterProfiles).toBeFalsy();

      expect(extensionInfo.capabilities.supportsPluginStatusReport).toBeTruthy();
      expect(extensionInfo.capabilities.supportsClusterStatusReport).toBeTruthy();
      expect(extensionInfo.capabilities.supportsAgentStatusReport).toBeTruthy();

      expect(extensionInfo.clusterProfileSettings).toEqual(undefined);

      expect(extensionInfo.pluginSettings!.viewTemplate())
        .toEqual(pluginInfoWithElasticAgentExtensionV4.extensions[0].plugin_settings!.view!.template);
      expect(extensionInfo.pluginSettings!.configurations().length).toEqual(1);
      expect(extensionInfo.pluginSettings!.configurations().map((config) => config.key)).toEqual(["instance_type"]);
      expect(extensionInfo.pluginSettings!.configurations()[0].metadata).toEqual({
                                                                                   secure: false,
                                                                                   required: true
                                                                                 });
    });

    it("should deserialize plugin info of v5 extension", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithElasticAgentExtensionV5);
      verifyBasicProperties(pluginInfo, pluginInfoWithElasticAgentExtensionV5);

      const extensionInfo   = pluginInfo.extensionOfType(ExtensionTypeString.ELASTIC_AGENTS)! as ElasticAgentExtension;
      const profileSettings = extensionInfo.profileSettings;
      expect(profileSettings.viewTemplate())
        .toEqual((pluginInfoWithElasticAgentExtensionV5.extensions[0] as ElasticAgentExtensionJSON).elastic_agent_profile_settings.view!.template);
      expect(profileSettings.configurations().length).toEqual(3);
      expect(profileSettings.configurations().map((config) => config.key))
        .toEqual(["Image", "Command", "Environment"]);
      expect(profileSettings.configurations()[0].metadata).toEqual({
                                                                     secure: false,
                                                                     required: true
                                                                   });

      expect(extensionInfo.supportsClusterProfiles).toBeTruthy();

      expect(extensionInfo.capabilities.supportsPluginStatusReport).toBeTruthy();
      expect(extensionInfo.capabilities.supportsClusterStatusReport).toBeTruthy();
      expect(extensionInfo.capabilities.supportsAgentStatusReport).toBeTruthy();

      expect(extensionInfo.clusterProfileSettings!.viewTemplate())
        .toEqual((pluginInfoWithElasticAgentExtensionV5.extensions[0] as ElasticAgentExtensionJSON).cluster_profile_settings!.view!.template);
      expect(extensionInfo.clusterProfileSettings!.configurations().length).toEqual(1);
      expect(extensionInfo.clusterProfileSettings!.configurations().map((config) => config.key))
        .toEqual(["instance_type"]);
      expect(extensionInfo.clusterProfileSettings!.configurations()[0].metadata).toEqual({
                                                                                           secure: false,
                                                                                           required: true
                                                                                         });
    });
  });

  describe("Notification", () => {
    it("should deserialize", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithNotificationExtension);
      verifyBasicProperties(pluginInfo, pluginInfoWithNotificationExtension);
      const extension = pluginInfo.extensionOfType(ExtensionTypeString.NOTIFICATION)!;

      expect(extension.pluginSettings!.viewTemplate())
        .toEqual((pluginInfoWithNotificationExtension.extensions[0] as NotificationExtensionJSON).plugin_settings!.view!.template);
    });
  });

  describe("PackageRepository", () => {
    it("should deserialize", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithPackageRepositoryExtension);

      verifyBasicProperties(pluginInfo, pluginInfoWithPackageRepositoryExtension);

      const extensionInfo = pluginInfo.extensionOfType(ExtensionTypeString.PACKAGE_REPO) as PackageRepoExtension;
      expect(extensionInfo.packageSettings.configurations().length).toEqual(4);
      expect(extensionInfo.packageSettings.configurations().map((config) => config.key))
        .toEqual(["PACKAGE_ID", "POLL_VERSION_FROM", "POLL_VERSION_TO", "INCLUDE_PRE_RELEASE"]);
      expect((extensionInfo.packageSettings.configurations()[0]).metadata as PackageMetadataJSON).toEqual({
                                                                                                            part_of_identity: true,
                                                                                                            display_order: 0,
                                                                                                            secure: false,
                                                                                                            display_name: "Package ID",
                                                                                                            required: true
                                                                                                          });

      expect(extensionInfo.repositorySettings.configurations().length).toEqual(3);
      expect(extensionInfo.repositorySettings.configurations().map((config) => config.key))
        .toEqual(["REPO_URL", "USERNAME", "PASSWORD"]);
      expect(extensionInfo.repositorySettings.configurations()[0].metadata as PackageMetadataJSON).toEqual({
                                                                                                             part_of_identity: true,
                                                                                                             display_order: 0,
                                                                                                             secure: false,
                                                                                                             display_name: "Repository Url",
                                                                                                             required: true
                                                                                                           });

      expect(extensionInfo.pluginSettings!.viewTemplate())
        .toEqual(pluginInfoWithPackageRepositoryExtension.extensions[0].plugin_settings!.view!.template);
      expect(extensionInfo.pluginSettings!.configurations().length).toEqual(1);
      expect(extensionInfo.pluginSettings!.configurations().map((config) => config.key)).toEqual(["another-property"]);
      expect(extensionInfo.pluginSettings!.configurations()[0].metadata).toEqual({
                                                                                   secure: false,
                                                                                   required: true
                                                                                 });
    });
  });

  describe("Task", () => {
    it("should deserialize", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithTaskExtension);
      verifyBasicProperties(pluginInfo, pluginInfoWithTaskExtension);

      const extensionInfo = pluginInfo.extensionOfType(ExtensionTypeString.TASK) as TaskExtension;
      expect(extensionInfo.taskSettings!.viewTemplate())
        .toEqual((pluginInfoWithTaskExtension.extensions[0] as TaskExtensionJSON).task_settings!.view!.template);
      expect(extensionInfo.taskSettings!.configurations().length).toEqual(4);
      expect(extensionInfo.taskSettings!.configurations().map((config) => config.key))
        .toEqual(["DockerFile", "DockerRunArguments", "IsDockerPush", "DockerBuildTag"]);
      expect(extensionInfo.taskSettings!.configurations()[0].metadata).toEqual({
                                                                                 secure: false,
                                                                                 required: false
                                                                               });
    });
  });

  describe("SCM", () => {
    it("should deserialize", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithSCMExtension);
      verifyBasicProperties(pluginInfo, pluginInfoWithSCMExtension);

      const extensionInfo = pluginInfo.extensionOfType(ExtensionTypeString.SCM) as ScmExtension;
      expect(extensionInfo.scmSettings!.viewTemplate())
        .toEqual((pluginInfoWithSCMExtension.extensions[0] as SCMExtensionJSON).scm_settings.view!.template);
      expect(extensionInfo.scmSettings!.configurations().length).toEqual(1);
      let keys = extensionInfo.scmSettings!.configurations().map((config) => config.key);
      expect(keys).toEqual(["url"]);
      expect(extensionInfo.scmSettings!.configurations()[0].metadata as SCMMetadataJSON).toEqual({
                                                                                                   part_of_identity: true,
                                                                                                   secure: false,
                                                                                                   required: true
                                                                                                 });

      expect(extensionInfo.pluginSettings!.viewTemplate())
        .toEqual((pluginInfoWithSCMExtension.extensions[0] as SCMExtensionJSON).plugin_settings!.view!.template);
      expect(extensionInfo.pluginSettings!.configurations().length).toEqual(1);
      keys = extensionInfo.pluginSettings!.configurations().map((config) => config.key);
      expect(keys).toEqual(["another-property"]);
      expect(extensionInfo.pluginSettings!.configurations()[0].metadata).toEqual({
                                                                                   secure: false,
                                                                                   required: true
                                                                                 });
    });
  });

  describe("Authorization", () => {
    it("should deserialize", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithAuthorizationExtension);
      verifyBasicProperties(pluginInfo, pluginInfoWithAuthorizationExtension);

      const extensionInfo = pluginInfo.extensionOfType(ExtensionTypeString.AUTHORIZATION) as AuthorizationExtension;
      expect(extensionInfo.authConfigSettings!.viewTemplate())
        .toEqual((pluginInfoWithAuthorizationExtension.extensions[0] as AuthorizationExtensionJSON).auth_config_settings.view!.template);
      expect(extensionInfo.authConfigSettings!.configurations().length).toEqual(4);
      expect(extensionInfo.authConfigSettings!.configurations().map((config) => config.key))
        .toEqual(["Url", "SearchBases", "ManagerDN", "Password"]);
      expect(extensionInfo.authConfigSettings!.configurations()[0].metadata).toEqual({
                                                                                       secure: false,
                                                                                       required: true
                                                                                     });

      expect(extensionInfo.roleSettings.viewTemplate())
        .toEqual((pluginInfoWithAuthorizationExtension.extensions[0] as AuthorizationExtensionJSON).role_settings.view!.template);
      expect(extensionInfo.roleSettings.configurations().length).toEqual(4);
      expect(extensionInfo.roleSettings.configurations().map((config) => config.key))
        .toEqual(["AttributeName", "AttributeValue", "GroupMembershipFilter", "GroupMembershipSearchBase"]);
      expect(extensionInfo.roleSettings.configurations()[0].metadata).toEqual({
                                                                                secure: false,
                                                                                required: false
                                                                              });

      expect(extensionInfo.capabilities.canAuthorize).toBeTruthy();
      expect(extensionInfo.capabilities.canSearch).toBeFalsy();
      expect(extensionInfo.capabilities.supportedAuthType).toEqual("web");
    });
  });

  describe("Artifact", () => {
    it("should deserialize", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithArtifactExtension);
      verifyBasicProperties(pluginInfo, pluginInfoWithArtifactExtension);

      const extensionInfo = pluginInfo.extensionOfType(ExtensionTypeString.ARTIFACT) as ArtifactExtension;
      expect(extensionInfo.storeConfigSettings!.viewTemplate())
        .toEqual((pluginInfoWithArtifactExtension.extensions[0] as ArtifactExtensionJSON).store_config_settings.view!.template);
      expect(extensionInfo.storeConfigSettings!.configurations().length).toEqual(3);
      expect(extensionInfo.storeConfigSettings!.configurations().map((config) => config.key))
        .toEqual(["S3_BUCKET", "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY"]);
      expect(extensionInfo.storeConfigSettings!.configurations()[0].metadata).toEqual({
                                                                                        secure: false,
                                                                                        required: true
                                                                                      });

      expect(extensionInfo.artifactConfigSettings.viewTemplate())
        .toEqual((pluginInfoWithArtifactExtension.extensions[0] as ArtifactExtensionJSON).artifact_config_settings.view!.template);
      expect(extensionInfo.artifactConfigSettings.configurations().length).toEqual(1);
      expect(extensionInfo.artifactConfigSettings.configurations().map((config) => config.key)).toEqual(["Filename"]);
      expect(extensionInfo.artifactConfigSettings.configurations()[0].metadata).toEqual({
                                                                                          secure: false,
                                                                                          required: false
                                                                                        });

      expect(extensionInfo.fetchArtifactSettings.viewTemplate())
        .toEqual((pluginInfoWithArtifactExtension.extensions[0] as ArtifactExtensionJSON).fetch_artifact_settings.view!.template);
      expect(extensionInfo.fetchArtifactSettings.configurations().length).toEqual(1);
      expect(extensionInfo.fetchArtifactSettings.configurations().map((config) => config.key)).toEqual(["Destination"]);
      expect(extensionInfo.fetchArtifactSettings.configurations()[0].metadata).toEqual({
                                                                                         secure: false,
                                                                                         required: false
                                                                                       });
    });
  });

  describe("ConfigRepo", () => {
    it("should deserialize", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithConfigRepoExtension);
      verifyBasicProperties(pluginInfo, pluginInfoWithConfigRepoExtension);
      const extension = pluginInfo.extensionOfType(ExtensionTypeString.CONFIG_REPO) as ConfigRepoExtension;

      expect(extension.pluginSettings!.viewTemplate())
        .toEqual((pluginInfoWithConfigRepoExtension.extensions[0] as ConfigRepoExtensionJSON).plugin_settings!.view!.template);
      expect(extension.pluginSettings!.configurations().length).toEqual(1);
      const keys = extension.pluginSettings!.configurations().map((config) => config.key);
      expect(keys).toEqual(["pipeline_pattern"]);
      expect(extension.pluginSettings!.configurations()[0].metadata).toEqual({
                                                                               secure: false,
                                                                               required: false
                                                                             });

    });
  });

  describe("Analytics", () => {
    it("should deserialize", () => {
      const pluginInfoJSON = AnalyticsPluginInfo.analytics();
      const pluginInfo     = PluginInfo.fromJSON(pluginInfoJSON);
      verifyBasicProperties(pluginInfo, pluginInfoJSON);
      const extension = pluginInfo.extensionOfType(ExtensionTypeString.ANALYTICS) as AnalyticsExtension;

      expect(extension.pluginSettings!.viewTemplate())
        .toEqual((pluginInfoJSON.extensions[0] as AnalyticsExtensionJSON).plugin_settings!.view!.template);
      expect(extension.pluginSettings!.configurations().length).toEqual(1);
      expect(extension.pluginSettings!.configurations().map((config) => config.key)).toEqual(["username"]);
      expect(extension.pluginSettings!.configurations()[0].metadata).toEqual({
                                                                               secure: false,
                                                                               required: true
                                                                             });

      expect(extension.capabilities.pipelineSupport()).toEqual([new AnalyticsCapability("rawr", "pipeline", "foo")]);
      expect(extension.capabilities.dashboardSupport())
        .toEqual([new AnalyticsCapability("foo", "dashboard", "something")]);
      expect(extension.capabilities.agentSupport()).toEqual([new AnalyticsCapability("bar", "agent", "bar")]);
    });
  });

  describe("Secrets", () => {
    it("should deserialize", () => {
      const secretConfigPluginInfoJson = SecretPluginInfo.file();
      const pluginInfo                 = PluginInfo.fromJSON(secretConfigPluginInfoJson);
      verifyBasicProperties(pluginInfo, secretConfigPluginInfoJson);
      const extension = pluginInfo.extensionOfType(ExtensionTypeString.SECRETS)! as SecretExtension;

      expect(extension.secretConfigSettings!.viewTemplate())
        .toEqual((secretConfigPluginInfoJson.extensions[0] as SecretConfigExtensionJSON).secret_config_settings.view!.template);
      expect(extension.secretConfigSettings!.configurations().length).toEqual(2);
      expect(extension.secretConfigSettings!.configurations().map((config) => config.key)).toEqual(["Url", "Token"]);
      expect(extension.secretConfigSettings!.configurations()[0].metadata).toEqual({
                                                                                     secure: false,
                                                                                     required: true
                                                                                   });

      expect(extension.secretConfigSettings!.configurations()[1].metadata).toEqual({
                                                                                     secure: true,
                                                                                     required: true
                                                                                   });
    });
  });

  describe("Reading images", () => {
    const json = {
      _links: {
        image: {
          href: "http://localhost:8153/go/api/plugin_images/cd.go.contrib.elastic-agent.ecs/ff36b7db1762e22ea7523980d90ffa5759bc7f08393be910601f15bfea1f4ca6"
        }
      },
      id: "github.pr",
      status: {
        state: "active"
      },
      about,
    } as PluginInfoJSON;

    _.each(_.values(ExtensionTypeString), (pluginType) => {
      it(`should read image for ${pluginType}`, () => {
        const pluginInfoJSON      = _.cloneDeep(json);
        pluginInfoJSON.extensions = [];
        const pluginInfo          = PluginInfo.fromJSON(pluginInfoJSON);
        expect(pluginInfo.imageUrl).toBe(json._links.image!.href);
      });
    });
  });

  describe("Multi-extension plugin", () => {
    let pluginInfoJSON: PluginInfoJSON;

    beforeEach(() => {
      pluginInfoJSON = {
        _links: pluginImageLink(),
        id: "multi.extension.plugin",
        plugin_file_location: "/foo/bar.jar",
        bundled_plugin: false,
        status: {
          state: "active"
        },
        about,
      } as PluginInfoJSON;
    });

    it("should deserialize", () => {
      pluginInfoJSON.extensions = [AnalyticsPluginInfo.analyticsExtension(), pluginInfoWithNotificationExtension.extensions[0], pluginInfoWithPackageRepositoryExtension.extensions[0]];

      const pluginInfo = PluginInfo.fromJSON(pluginInfoJSON);
      verifyBasicProperties(pluginInfo, pluginInfoJSON);

      expect(pluginInfo.types()).toEqual(["analytics", "notification", "package-repository"]);

      expect(pluginInfo.extensionOfType(ExtensionTypeString.NOTIFICATION)!.pluginSettings!.configurations().length)
        .toEqual(1);

      const analyticsExtensionInfo = pluginInfo.extensionOfType(ExtensionTypeString.ANALYTICS)! as AnalyticsExtension;
      expect(analyticsExtensionInfo.capabilities.pipelineSupport())
        .toEqual([new AnalyticsCapability("rawr", "pipeline", "foo")]);
      expect(analyticsExtensionInfo.capabilities.dashboardSupport())
        .toEqual([new AnalyticsCapability("foo", "dashboard", "something")]);

      const packageRepositoryExtensionInfo = pluginInfo.extensionOfType(ExtensionTypeString.PACKAGE_REPO)! as PackageRepoExtension;
      expect(packageRepositoryExtensionInfo.packageSettings.configurations().length).toEqual(4);
      expect(packageRepositoryExtensionInfo.repositorySettings.configurations().length).toEqual(3);
    });

    it("should find the first extension with the plugin settings to use as settings for the plugin", () => {
      pluginInfoJSON.extensions = [pluginInfoWithNotificationExtension.extensions[0], AnalyticsPluginInfo.analyticsExtension()];

      const pluginInfo = PluginInfo.fromJSON(pluginInfoJSON);
      expect(pluginInfo.firstExtensionWithPluginSettings()!.pluginSettings!.viewTemplate())
        .toEqual(pluginInfoWithNotificationExtension.extensions[0].plugin_settings!.view!.template);
    });
  });

  describe("Invalid plugin", () => {
    let pluginInfoWithErrors: PluginInfoJSON, pluginInfoWithoutAboutInfo: PluginInfoJSON;

    beforeEach(() => {
      pluginInfoWithErrors = {
        _links: {
          self: {
            href: "http://localhost:8153/go/api/admin/plugin_info/test-plugin-xml"
          },
          doc: {
            href: "https://api.gocd.org/#plugin-info"
          },
          find: {
            href: "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
          }
        } as LinksJSON,
        id: "test-plugin-xml",
        status: {
          state: "invalid",
          messages: [
            "Plugin with ID (test-plugin-xml) is not valid: Incompatible with current operating system 'Mac OS X'. Valid operating systems are: [Windows]."
          ]
        },
        plugin_file_location: "/Users/ganeshp/projects/gocd/gocd/server/plugins/external/test-with-some-plugin-xml-values.jar",
        bundled_plugin: false,
        about: {
          version: "1.0.0",
          description: "Plugin that has only some fields in its plugin.xml",
          target_operating_systems: [
            "Windows"
          ],
          vendor: {
            url: "www.mdaliejaz.com"
          } as VendorJSON
        } as AboutJSON,
        extensions: []
      };

      pluginInfoWithoutAboutInfo = {
        _links: {
          self: {
            href: "http://localhost:8153/go/api/admin/plugin_info/plugin-common.jar"
          },
          doc: {
            href: "https://api.gocd.org/#plugin-info"
          },
          find: {
            href: "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
          }
        } as LinksJSON,
        id: "plugin-common.jar",
        status: {
          state: "invalid",
          messages: [
            "No extensions found in this plugin.Please check for @Extension annotations"
          ]
        },
        plugin_file_location: "/Users/ganeshp/projects/gocd/gocd/server/plugins/external/plugin-common.jar",
        bundled_plugin: false,
        extensions: []
      } as unknown as PluginInfoJSON;
    });

    it("should deserialize plugin info having errors", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithErrors);

      expect(pluginInfo.status.state).toEqual(pluginInfoWithErrors.status.state);
      expect(pluginInfo.status.messages).toEqual(pluginInfoWithErrors.status.messages);
      expect(pluginInfo.about.name).toBeUndefined();
      expect(pluginInfo.about.version).toEqual(pluginInfoWithErrors.about!.version);
      expect(pluginInfo.about.targetGoVersion).toBeUndefined();
      expect(pluginInfo.about.description).toEqual(pluginInfoWithErrors.about!.description);
      expect(pluginInfo.about.targetOperatingSystems).toEqual(pluginInfoWithErrors.about!.target_operating_systems);
      expect(pluginInfo.about.vendor.name).toBeUndefined();
      expect(pluginInfo.about.vendor.url).toEqual(pluginInfoWithErrors.about!.vendor.url);
    });

    it("should deserialize plugin info not containing about information", () => {
      const pluginInfo = PluginInfo.fromJSON(pluginInfoWithoutAboutInfo);

      expect(pluginInfo.status.state).toEqual(pluginInfoWithoutAboutInfo.status.state);
      expect(pluginInfo.status.messages).toEqual(pluginInfoWithoutAboutInfo.status.messages);
      expect(pluginInfo.about.name).toBeUndefined();
      expect(pluginInfo.about.version).toBeUndefined();
      expect(pluginInfo.about.targetGoVersion).toBeUndefined();
      expect(pluginInfo.about.description).toBeUndefined();
      expect(pluginInfo.about.targetOperatingSystems).toBeUndefined();
      expect(pluginInfo.about.vendor.name).toBeUndefined();
      expect(pluginInfo.about.vendor.url).toBeUndefined();
    });
  });

  const verifyBasicProperties = (pluginInfo: PluginInfo,
                                 pluginInfoJSON: PluginInfoJSON) => {
    expect(pluginInfo.id).toEqual(pluginInfoJSON.id);
    expect(pluginInfo.types()).toContain(pluginInfoJSON.extensions[0].type);
    expect(pluginInfo.status.state).toEqual(pluginInfoJSON.status.state);
    expect(pluginInfo.status.messages).toEqual(pluginInfoJSON.status.messages);
    expect(pluginInfo.about.name).toEqual(pluginInfoJSON.about!.name);
    expect(pluginInfo.about.version).toEqual(pluginInfoJSON.about!.version);
    expect(pluginInfo.about.targetGoVersion).toEqual(pluginInfoJSON.about!.target_go_version);
    expect(pluginInfo.about.description).toEqual(pluginInfoJSON.about!.description);
    expect(pluginInfo.about.targetOperatingSystems).toEqual(pluginInfoJSON.about!.target_operating_systems);
    expect(pluginInfo.about.vendor.name).toEqual(pluginInfoJSON.about!.vendor.name);
    expect(pluginInfo.about.vendor.url).toEqual(pluginInfoJSON.about!.vendor.url);
  };
});
