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

import {Stream} from "mithril/stream";
import {AnalyticsCapabilities} from "./analytics_plugin_capabilities";
import {AuthCapabilities} from "./authorization_plugin_capabilities";
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

  static fromJSON(data: any): PluginSettings {
    let configurations;
    if (data && data.configurations) {
      configurations = data.configurations.map((config: any) => Configuration.fromJSON(config));
    }
    const viewTemplate = data && data.view ? data.view.template : undefined;
    return new PluginSettings(configurations, viewTemplate);
  }
}

export abstract class Extension {
  readonly type: ExtensionType;

  protected constructor(type: ExtensionType) {
    this.type = type;
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
    }
  }

  abstract hasView(): boolean;

  abstract hasConfigurations(): boolean;

  abstract hasPluginSettings(): boolean;

}

class ArtifactSettings extends Extension {

  readonly storeConfigSettings: PluginSettings;
  readonly artifactConfigSettings: PluginSettings;
  readonly fetchArtifactSettings: PluginSettings;

  constructor(storeConfigSettings: PluginSettings, artifactConfigSettings: PluginSettings, fetchArtifactSettings: PluginSettings) {
    super(ExtensionType.ARTIFACT);
    this.storeConfigSettings    = storeConfigSettings;
    this.artifactConfigSettings = artifactConfigSettings;
    this.fetchArtifactSettings  = fetchArtifactSettings;
  }

  static fromJSON(data: any) {
    return new ArtifactSettings(PluginSettings.fromJSON(data.store_config_settings),
      PluginSettings.fromJSON(data.artifact_config_settings), PluginSettings.fromJSON(data.fetch_artifact_settings));
  }

  hasConfigurations(): boolean {
    return true;
  }

  hasView(): boolean {
    return true;
  }

  hasPluginSettings(): boolean {
    return false;
  }

}

class ConfigRepoSettings extends Extension {
  readonly pluginSettings: PluginSettings;

  constructor(pluginSettings: PluginSettings) {
    super(ExtensionType.CONFIG_REPO);
    this.pluginSettings = pluginSettings;
  }

  static fromJSON(data: any) {
    return new ConfigRepoSettings(PluginSettings.fromJSON(data.plugin_settings));
  }

  hasConfigurations(): boolean {
    return this.pluginSettings.configurations !== undefined && this.pluginSettings.configurations.length > 0;
  }

  hasView(): boolean {
    return this.pluginSettings.viewTemplate !== undefined;
  }

  hasPluginSettings(): boolean {
    return true;
  }
}

class ElasticAgentSettings extends Extension {
  readonly pluginSettings: PluginSettings;
  readonly profileSettings: PluginSettings;
  readonly capabilities: ElasticPluginCapabilities;

  constructor(pluginSettings: PluginSettings, profileSettings: PluginSettings, capabilities: ElasticPluginCapabilities) {
    super(ExtensionType.ELASTIC_AGENTS);
    this.pluginSettings  = pluginSettings;
    this.profileSettings = profileSettings;
    this.capabilities    = capabilities;
  }

  static fromJSON(data: any) {
    return new ElasticAgentSettings(PluginSettings.fromJSON(data.plugin_settings),
      PluginSettings.fromJSON(data.profile_settings), ElasticPluginCapabilities.fromJSON(data.capabilities));
  }

  hasConfigurations(): boolean {
    return this.pluginSettings.configurations !== undefined && this.pluginSettings.configurations.length > 0;
  }

  hasView(): boolean {
    return this.pluginSettings.viewTemplate !== undefined;
  }

  hasPluginSettings(): boolean {
    return true;
  }
}

class AuthorizationSettings extends Extension {
  readonly authConfigSettings: PluginSettings;
  readonly roleSettings: PluginSettings;
  readonly capabilities: AuthCapabilities;

  constructor(authConfigSettings: PluginSettings, roleSettings: PluginSettings, capabilities: AuthCapabilities) {
    super(ExtensionType.AUTHORIZATION);
    this.authConfigSettings = authConfigSettings;
    this.roleSettings       = roleSettings;
    this.capabilities       = capabilities;
  }

  static fromJSON(data: any) {
    return new AuthorizationSettings(PluginSettings.fromJSON(data.auth_config_settings),
      PluginSettings.fromJSON(data.role_settings), AuthCapabilities.fromJSON(data.capabilities));
  }

  hasConfigurations(): boolean {
    return this.authConfigSettings.configurations !== undefined && this.authConfigSettings.configurations.length > 0;
  }

  hasView(): boolean {
    return this.authConfigSettings.viewTemplate !== undefined;
  }

  hasPluginSettings(): boolean {
    return false;
  }
}

class ScmSettings extends Extension {
  readonly displayName: string;
  readonly scmSettings: PluginSettings;

  constructor(displayName: string, scmSettings: PluginSettings) {
    super(ExtensionType.SCM);
    this.displayName = displayName;
    this.scmSettings = scmSettings;
  }

  static fromJSON(data: any) {
    return new ScmSettings(data.display_name, PluginSettings.fromJSON(data.scm_settings));
  }

  hasConfigurations(): boolean {
    return false;
  }

  hasView(): boolean {
    return false;
  }

  hasPluginSettings(): boolean {
    return false;
  }
}

class TaskSettings extends Extension {
  readonly displayName: string;
  readonly taskSettings: PluginSettings;

  constructor(displayName: string, taskSettings: PluginSettings) {
    super(ExtensionType.TASK);
    this.displayName  = displayName;
    this.taskSettings = taskSettings;
  }

  static fromJSON(data: any) {
    return new TaskSettings(data.display_name, PluginSettings.fromJSON(data.task_settings));
  }

  hasConfigurations(): boolean {
    //return this.taskSettings.configurations != undefined && this.taskSettings.configurations.length > 0;
    return false;
  }

  hasView(): boolean {
    //return this.taskSettings.viewTemplate != undefined;
    return false;
  }

  hasPluginSettings(): boolean {
    return false;
  }
}

class PackageRepoSettings extends Extension {
  readonly packageSettings: PluginSettings;
  readonly repositorySettings: PluginSettings;
  readonly pluginSettings: PluginSettings;

  constructor(packageSettings: PluginSettings, repositorySettings: PluginSettings, pluginSettings: PluginSettings) {
    super(ExtensionType.PACKAGE_REPO);
    this.packageSettings    = packageSettings;
    this.repositorySettings = repositorySettings;
    this.pluginSettings     = pluginSettings;
  }

  static fromJSON(data: any) {
    return new PackageRepoSettings(PluginSettings.fromJSON(data.package_settings),
      PluginSettings.fromJSON(data.repository_settings), PluginSettings.fromJSON(data.plugin_settings));
  }

  hasConfigurations(): boolean {
    return false;
  }

  hasView(): boolean {
    return false;
  }

  hasPluginSettings(): boolean {
    return true;
  }
}

class NotificationSettings extends Extension {
  readonly pluginSettings: PluginSettings;

  constructor(pluginSettings: PluginSettings) {
    super(ExtensionType.NOTIFICATION);
    this.pluginSettings = pluginSettings;
  }

  static fromJSON(data: any) {
    return new NotificationSettings(PluginSettings.fromJSON(data.plugin_settings));
  }

  hasConfigurations(): boolean {
    return this.pluginSettings.configurations !== undefined && this.pluginSettings.configurations.length > 0;
  }

  hasView(): boolean {
    return this.pluginSettings.viewTemplate !== undefined;
  }

  hasPluginSettings(): boolean {
    return true;
  }
}

class AnalyticsSettings extends Extension {
  readonly pluginSettings: PluginSettings;
  readonly capabilities: AnalyticsCapabilities;

  constructor(pluginSettings: PluginSettings, capabilities: AnalyticsCapabilities) {
    super(ExtensionType.ANALYTICS);
    this.pluginSettings = pluginSettings;
    this.capabilities   = capabilities;
  }

  static fromJSON(data: any) {
    return new AnalyticsSettings(PluginSettings.fromJSON(data.plugin_settings),
      AnalyticsCapabilities.fromJSON(data.capabilities));
  }

  hasConfigurations(): boolean {
    return this.pluginSettings.configurations !== undefined && this.pluginSettings.configurations.length > 0;
  }

  hasView(): boolean {
    return this.pluginSettings.viewTemplate !== undefined;
  }

  hasPluginSettings(): boolean {
    return true;
  }
}
