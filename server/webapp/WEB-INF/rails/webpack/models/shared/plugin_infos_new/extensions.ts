/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import {Stream} from "mithril/stream";
import {AnalyticsCapabilities} from "./analytics_plugin_capabilities";
import {AuthCapabilities} from "./authorization_plugin_capabilities";
import {ConfigRepoCapabilities} from "./config_repo_capabilities";
import {ElasticPluginCapabilities} from "./elastic_plugin_capabilities";
import {ExtensionType} from "./extension_type";

const stream = require("mithril/stream");

class Configuration {
  readonly key: string;
  readonly metadata: any;

  constructor(key: any, metadata: any) {
    this.key      = key;
    this.metadata = metadata;
  }

  static fromJSON(data: any) {
    return new Configuration(data.key, data.metadata);
  }
}

class PluginSettings {
  configurations?: Stream<Configuration[]>;
  viewTemplate?: Stream<string>;

  constructor(configurations: Configuration[], viewTemplate: string) {
    this.configurations = stream(configurations);
    this.viewTemplate   = stream(viewTemplate);
  }

  static tryParsing(data: any) {
    if (data.plugin_settings) {
      return PluginSettings.fromJSON(data.plugin_settings);
    }
  }

  static fromJSON(pluginSettingsJson: any): PluginSettings {
    let configurations;
    if (pluginSettingsJson && pluginSettingsJson.configurations) {
      configurations = pluginSettingsJson.configurations.map((config: any) => Configuration.fromJSON(config));
    }
    const viewTemplate = pluginSettingsJson && pluginSettingsJson.view ? pluginSettingsJson.view.template : undefined;
    return new PluginSettings(configurations, viewTemplate);
  }

  hasView() {
    return this.viewTemplate !== undefined && this.viewTemplate() !== undefined && this.viewTemplate()
                                                                                       .trim().length > 0;
  }

  hasConfigurations() {
    return this.configurations !== undefined && this.configurations() !== undefined && this.configurations().length > 0;
  }
}

export abstract class Extension {
  readonly type: ExtensionType;
  readonly pluginSettings?: PluginSettings;

  protected constructor(type: ExtensionType, pluginSettings?: PluginSettings) {
    this.type           = type;
    this.pluginSettings = pluginSettings;
  }

  static fromJSON(data: any): Extension | undefined {
    const type: ExtensionType = data.type;
    switch (type) {
      case ExtensionType.ARTIFACT:
        return ArtifactSettings.fromJSON(data);
      case ExtensionType.ANALYTICS:
        return AnalyticsSettings.fromJSON(data);
      case ExtensionType.AUTHORIZATION:
        return AuthorizationSettings.fromJSON(data);
      case ExtensionType.CONFIG_REPO:
        return ConfigRepoSettings.fromJSON(data);
      case ExtensionType.ELASTIC_AGENTS:
        return ElasticAgentSettings.fromJSON(data);
      case ExtensionType.NOTIFICATION:
        return NotificationSettings.fromJSON(data);
      case ExtensionType.PACKAGE_REPO:
        return PackageRepoSettings.fromJSON(data);
      case ExtensionType.SCM:
        return ScmSettings.fromJSON(data);
      case ExtensionType.TASK:
        return TaskSettings.fromJSON(data);
      case ExtensionType.SECRETS:
        return SecretSettings.fromJSON(data);
    }
  }

  hasView(): boolean {
    return this.pluginSettings !== undefined && this.pluginSettings.hasView();
  }

  hasConfigurations(): boolean {
    return this.pluginSettings !== undefined && this.pluginSettings.hasConfigurations();
  }

  hasPluginSettings(): boolean {
    return this.hasView() && this.hasConfigurations();
  }

}

export class ArtifactSettings extends Extension {

  readonly storeConfigSettings: PluginSettings;
  readonly artifactConfigSettings: PluginSettings;
  readonly fetchArtifactSettings: PluginSettings;

  constructor(storeConfigSettings: PluginSettings,
              artifactConfigSettings: PluginSettings,
              fetchArtifactSettings: PluginSettings,
              pluginSettings?: PluginSettings) {
    super(ExtensionType.ARTIFACT, pluginSettings);
    this.storeConfigSettings    = storeConfigSettings;
    this.artifactConfigSettings = artifactConfigSettings;
    this.fetchArtifactSettings  = fetchArtifactSettings;
  }

  static fromJSON(data: any) {
    return new ArtifactSettings(PluginSettings.fromJSON(data.store_config_settings),
                                PluginSettings.fromJSON(data.artifact_config_settings),
                                PluginSettings.fromJSON(data.fetch_artifact_settings),
                                PluginSettings.tryParsing(data));
  }
}

export class ConfigRepoSettings extends Extension {
  readonly capabilities: ConfigRepoCapabilities;

  constructor(capabilities: ConfigRepoCapabilities, pluginSettings?: PluginSettings) {
    super(ExtensionType.CONFIG_REPO, pluginSettings);
    this.capabilities = capabilities;
  }

  static fromJSON(data: any) {
    return new ConfigRepoSettings(
      ConfigRepoCapabilities.fromJSON(data.capabilities),
      PluginSettings.fromJSON(data.plugin_settings),
    );
  }
}

export class ElasticAgentSettings extends Extension {
  readonly profileSettings: PluginSettings;
  readonly capabilities: ElasticPluginCapabilities;
  readonly clusterProfileSettings?: PluginSettings;
  readonly supportsClusterProfiles: boolean;

  constructor(supportsClusterProfiles: boolean,
              profileSettings: PluginSettings,
              capabilities: ElasticPluginCapabilities,
              pluginSettings?: PluginSettings,
              clusterProfileSettings?: PluginSettings) {
    super(ExtensionType.ELASTIC_AGENTS, pluginSettings);
    this.supportsClusterProfiles = supportsClusterProfiles;
    this.profileSettings         = profileSettings;
    this.clusterProfileSettings  = clusterProfileSettings;
    this.capabilities            = capabilities;
  }

  static fromJSON(data: any) {
    const supportsClusterProfiles: boolean = data.supports_cluster_profiles;

    let clusterProfileSettings: PluginSettings | undefined;

    if (supportsClusterProfiles) {
      clusterProfileSettings = PluginSettings.fromJSON(data.cluster_profile_settings);
    }

    return new ElasticAgentSettings(
      supportsClusterProfiles,
      PluginSettings.fromJSON(data.elastic_agent_profile_settings),
      ElasticPluginCapabilities.fromJSON(data.capabilities),
      PluginSettings.tryParsing(data),
      clusterProfileSettings
    );
  }
}

export class AuthorizationSettings extends Extension {
  readonly authConfigSettings: PluginSettings;
  readonly roleSettings: PluginSettings;
  readonly capabilities: AuthCapabilities;

  constructor(authConfigSettings: PluginSettings,
              roleSettings: PluginSettings,
              capabilities: AuthCapabilities,
              pluginSettings?: PluginSettings) {
    super(ExtensionType.AUTHORIZATION, pluginSettings);
    this.authConfigSettings = authConfigSettings;
    this.roleSettings       = roleSettings;
    this.capabilities       = capabilities;
  }

  static fromJSON(data: any) {
    return new AuthorizationSettings(PluginSettings.fromJSON(data.auth_config_settings),
                                     PluginSettings.fromJSON(data.role_settings),
                                     AuthCapabilities.fromJSON(data.capabilities),
                                     PluginSettings.tryParsing(data));
  }
}

class ScmSettings extends Extension {
  readonly displayName: string;
  readonly scmSettings: PluginSettings;

  constructor(displayName: string, scmSettings: PluginSettings, pluginSettings?: PluginSettings) {
    super(ExtensionType.SCM, pluginSettings);
    this.displayName = displayName;
    this.scmSettings = scmSettings;
  }

  static fromJSON(data: any) {
    return new ScmSettings(data.display_name,
                           PluginSettings.fromJSON(data.scm_settings),
                           PluginSettings.tryParsing(data));
  }
}

class TaskSettings extends Extension {
  readonly displayName: string;
  readonly taskSettings: PluginSettings;

  constructor(displayName: string, taskSettings: PluginSettings, pluginSettings?: PluginSettings) {
    super(ExtensionType.TASK, pluginSettings);
    this.displayName  = displayName;
    this.taskSettings = taskSettings;
  }

  static fromJSON(data: any) {
    return new TaskSettings(data.display_name,
                            PluginSettings.fromJSON(data.task_settings),
                            PluginSettings.tryParsing(data));
  }
}

class PackageRepoSettings extends Extension {
  readonly packageSettings: PluginSettings;
  readonly repositorySettings: PluginSettings;

  constructor(packageSettings: PluginSettings, repositorySettings: PluginSettings, pluginSettings?: PluginSettings) {
    super(ExtensionType.PACKAGE_REPO, pluginSettings);
    this.packageSettings    = packageSettings;
    this.repositorySettings = repositorySettings;
  }

  static fromJSON(data: any) {
    return new PackageRepoSettings(PluginSettings.fromJSON(data.package_settings),
                                   PluginSettings.fromJSON(data.repository_settings), PluginSettings.tryParsing(data));
  }
}

class NotificationSettings extends Extension {

  constructor(pluginSettings?: PluginSettings) {
    super(ExtensionType.NOTIFICATION, pluginSettings);
  }

  static fromJSON(data: any) {
    return new NotificationSettings(PluginSettings.tryParsing(data));
  }
}

class AnalyticsSettings extends Extension {
  readonly capabilities: AnalyticsCapabilities;

  constructor(capabilities: AnalyticsCapabilities, pluginSettings?: PluginSettings) {
    super(ExtensionType.ANALYTICS, pluginSettings);
    this.capabilities = capabilities;
  }

  static fromJSON(data: any) {
    return new AnalyticsSettings(AnalyticsCapabilities.fromJSON(data.capabilities), PluginSettings.tryParsing(data));
  }
}

export class SecretSettings extends Extension {
  readonly secretConfigSettings: PluginSettings;

  constructor(secretConfigSettings: PluginSettings,
              pluginSettings?: PluginSettings) {
    super(ExtensionType.SECRETS, pluginSettings);
    this.secretConfigSettings = secretConfigSettings;
  }

  static fromJSON(data: any) {
    return new SecretSettings(PluginSettings.fromJSON(data.secret_config_settings), PluginSettings.tryParsing(data));
  }
}
