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
import Stream from "mithril/stream";
import {
  AnalyticsExtensionJSON,
  ArtifactExtensionJSON,
  AuthorizationExtensionJSON,
  ConfigRepoExtensionJSON, ConfigurationJSON,
  ElasticAgentExtensionJSON,
  ExtensionJSON, HasPluginSettings, MetadataJSON,
  NotificationExtensionJSON,
  PackageRepoExtensionJSON, PluginSettingJSON,
  SCMExtensionJSON,
  SecretConfigExtensionJSON,
  TaskExtensionJSON
} from "models/shared/plugin_infos_new/serialization";
import s from "underscore.string";
import {AnalyticsCapabilities} from "./analytics_plugin_capabilities";
import {AuthCapabilities} from "./authorization_plugin_capabilities";
import {ConfigRepoCapabilities} from "./config_repo_capabilities";
import {ElasticPluginCapabilities} from "./elastic_plugin_capabilities";
import {ExtensionType, ExtensionTypeString} from "./extension_type";

class Configuration {
  readonly key: string;
  readonly metadata: MetadataJSON;

  constructor(key: string, metadata: MetadataJSON) {
    this.key      = key;
    this.metadata = metadata;
  }

  static fromJSON(data: ConfigurationJSON) {
    return new Configuration(data.key, data.metadata);
  }
}

class PluginSettings {
  readonly configurations: Stream<Configuration[]>;
  readonly viewTemplate: Stream<string | undefined>;

  constructor(configurations: Configuration[], viewTemplate?: string) {
    this.configurations = Stream(configurations || []);
    this.viewTemplate   = Stream(viewTemplate);
  }

  static tryParsing(data: HasPluginSettings) {
    if (data.plugin_settings) {
      return PluginSettings.fromJSON(data.plugin_settings);
    }
  }

  static fromJSON(pluginSettingsJson: PluginSettingJSON): PluginSettings {
    let configurations: Configuration[] = [];
    if (pluginSettingsJson && pluginSettingsJson.configurations) {
      configurations = pluginSettingsJson.configurations.map((config: ConfigurationJSON) => Configuration.fromJSON(
        config));
    }
    const viewTemplate = pluginSettingsJson && pluginSettingsJson.view ? pluginSettingsJson.view.template : undefined;
    return new PluginSettings(configurations, viewTemplate);
  }

  hasView() {
     return !s.isBlank(this.viewTemplate() as string);
  }

  hasConfigurations() {
    return this.configurations !== undefined && this.configurations() !== undefined && this.configurations().length > 0;
  }
}

export abstract class Extension {
  readonly type: ExtensionTypeString;
  readonly pluginSettings?: PluginSettings;

  protected constructor(type: ExtensionTypeString, pluginSettings?: PluginSettings) {
    this.type           = type;
    this.pluginSettings = pluginSettings;
  }

  static fromJSON(data: ExtensionJSON): Extension {
    return ExtensionType.fromString(data.type).fromJSON(data);
  }

  hasView(): boolean {
    return this.pluginSettings ? this.pluginSettings.hasView() : false;
  }

  hasConfigurations(): boolean {
    return this.pluginSettings !== undefined && this.pluginSettings.hasConfigurations();
  }

  hasPluginSettings(): boolean {
    return this.hasView() && this.hasConfigurations();
  }

}

export class ArtifactExtension extends Extension {

  readonly storeConfigSettings: PluginSettings;
  readonly artifactConfigSettings: PluginSettings;
  readonly fetchArtifactSettings: PluginSettings;

  constructor(storeConfigSettings: PluginSettings,
              artifactConfigSettings: PluginSettings,
              fetchArtifactSettings: PluginSettings,
              pluginSettings?: PluginSettings) {
    super(ExtensionTypeString.ARTIFACT, pluginSettings);
    this.storeConfigSettings    = storeConfigSettings;
    this.artifactConfigSettings = artifactConfigSettings;
    this.fetchArtifactSettings  = fetchArtifactSettings;
  }

  static fromJSON(data: ArtifactExtensionJSON) {
    return new ArtifactExtension(PluginSettings.fromJSON(data.store_config_settings),
                                 PluginSettings.fromJSON(data.artifact_config_settings),
                                 PluginSettings.fromJSON(data.fetch_artifact_settings),
                                 PluginSettings.tryParsing(data));
  }
}

export class ConfigRepoExtension extends Extension {
  readonly capabilities: ConfigRepoCapabilities;

  constructor(capabilities: ConfigRepoCapabilities, pluginSettings?: PluginSettings) {
    super(ExtensionTypeString.CONFIG_REPO, pluginSettings);
    this.capabilities = capabilities;
  }

  static fromJSON(data: ConfigRepoExtensionJSON) {
    return new ConfigRepoExtension(
      ConfigRepoCapabilities.fromJSON(data.capabilities),
      PluginSettings.fromJSON(data.plugin_settings || {}),
    );
  }
}

export class ElasticAgentExtension extends Extension {
  readonly profileSettings: PluginSettings;
  readonly capabilities: ElasticPluginCapabilities;
  readonly clusterProfileSettings?: PluginSettings;
  readonly supportsClusterProfiles: boolean;

  constructor(supportsClusterProfiles: boolean,
              profileSettings: PluginSettings,
              capabilities: ElasticPluginCapabilities,
              pluginSettings?: PluginSettings,
              clusterProfileSettings?: PluginSettings) {
    super(ExtensionTypeString.ELASTIC_AGENTS, pluginSettings);
    this.supportsClusterProfiles = supportsClusterProfiles;
    this.profileSettings         = profileSettings;
    this.clusterProfileSettings  = clusterProfileSettings;
    this.capabilities            = capabilities;
  }

  static fromJSON(data: ElasticAgentExtensionJSON) {
    const supportsClusterProfiles = data.supports_cluster_profiles;

    let clusterProfileSettings: PluginSettings | undefined;

    if (supportsClusterProfiles) {
      clusterProfileSettings = PluginSettings.fromJSON(data.cluster_profile_settings!);
    }

    return new ElasticAgentExtension(
      supportsClusterProfiles,
      PluginSettings.fromJSON(data.elastic_agent_profile_settings),
      ElasticPluginCapabilities.fromJSON(data.capabilities),
      PluginSettings.tryParsing(data),
      clusterProfileSettings
    );
  }
}

export class AuthorizationExtension extends Extension {
  readonly authConfigSettings: PluginSettings;
  readonly roleSettings: PluginSettings;
  readonly capabilities: AuthCapabilities;

  constructor(authConfigSettings: PluginSettings,
              roleSettings: PluginSettings,
              capabilities: AuthCapabilities,
              pluginSettings?: PluginSettings) {
    super(ExtensionTypeString.AUTHORIZATION, pluginSettings);
    this.authConfigSettings = authConfigSettings;
    this.roleSettings       = roleSettings;
    this.capabilities       = capabilities;
  }

  static fromJSON(data: AuthorizationExtensionJSON) {
    return new AuthorizationExtension(PluginSettings.fromJSON(data.auth_config_settings),
                                      PluginSettings.fromJSON(data.role_settings),
                                      AuthCapabilities.fromJSON(data.capabilities),
                                      PluginSettings.tryParsing(data));
  }
}

export class ScmExtension extends Extension {
  readonly displayName: string;
  readonly scmSettings: PluginSettings;

  constructor(displayName: string, scmSettings: PluginSettings, pluginSettings?: PluginSettings) {
    super(ExtensionTypeString.SCM, pluginSettings);
    this.displayName = displayName;
    this.scmSettings = scmSettings;
  }

  static fromJSON(data: SCMExtensionJSON) {
    return new ScmExtension(data.display_name,
                            PluginSettings.fromJSON(data.scm_settings),
                            PluginSettings.tryParsing(data));
  }
}

export class TaskExtension extends Extension {
  readonly displayName: string;
  readonly taskSettings: PluginSettings;

  constructor(displayName: string, taskSettings: PluginSettings, pluginSettings?: PluginSettings) {
    super(ExtensionTypeString.TASK, pluginSettings);
    this.displayName  = displayName;
    this.taskSettings = taskSettings;
  }

  static fromJSON(data: TaskExtensionJSON) {
    return new TaskExtension(data.display_name,
                             PluginSettings.fromJSON(data.task_settings),
                             PluginSettings.tryParsing(data));
  }
}

export class PackageRepoExtension extends Extension {
  readonly packageSettings: PluginSettings;
  readonly repositorySettings: PluginSettings;

  constructor(packageSettings: PluginSettings, repositorySettings: PluginSettings, pluginSettings?: PluginSettings) {
    super(ExtensionTypeString.PACKAGE_REPO, pluginSettings);
    this.packageSettings    = packageSettings;
    this.repositorySettings = repositorySettings;
  }

  static fromJSON(data: PackageRepoExtensionJSON) {
    return new PackageRepoExtension(PluginSettings.fromJSON(data.package_settings),
                                    PluginSettings.fromJSON(data.repository_settings), PluginSettings.tryParsing(data));
  }
}

export class NotificationExtension extends Extension {

  constructor(pluginSettings?: PluginSettings) {
    super(ExtensionTypeString.NOTIFICATION, pluginSettings);
  }

  static fromJSON(data: NotificationExtensionJSON) {
    return new NotificationExtension(PluginSettings.tryParsing(data));
  }
}

export class AnalyticsExtension extends Extension {
  readonly capabilities: AnalyticsCapabilities;

  constructor(capabilities: AnalyticsCapabilities, pluginSettings?: PluginSettings) {
    super(ExtensionTypeString.ANALYTICS, pluginSettings);
    this.capabilities = capabilities;
  }

  static fromJSON(data: AnalyticsExtensionJSON) {
    return new AnalyticsExtension(AnalyticsCapabilities.fromJSON(data.capabilities), PluginSettings.tryParsing(data));
  }
}

export class SecretExtension extends Extension {
  readonly secretConfigSettings: PluginSettings;

  constructor(secretConfigSettings: PluginSettings,
              pluginSettings?: PluginSettings) {
    super(ExtensionTypeString.SECRETS, pluginSettings);
    this.secretConfigSettings = secretConfigSettings;
  }

  static fromJSON(data: SecretConfigExtensionJSON) {
    return new SecretExtension(PluginSettings.fromJSON(data.secret_config_settings), PluginSettings.tryParsing(data));
  }
}
