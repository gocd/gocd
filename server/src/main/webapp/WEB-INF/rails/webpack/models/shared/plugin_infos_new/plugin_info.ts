/*
 * Copyright 2021 ThoughtWorks, Inc.
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
  AboutJSON,
  ExtensionJSON,
  PluginInfoJSON,
  PluginInfosJSON,
  PluginState,
  StatusJSON
} from "models/shared/plugin_infos_new/serialization";
import {About} from "./about";
import {ConfigRepoCapabilities} from "./config_repo_capabilities";
import {AuthorizationExtension, ConfigRepoExtension, Extension} from "./extensions";
import {ExtensionType, ExtensionTypeString} from "./extension_type";

class Status {
  readonly state: PluginState;
  readonly messages?: string[];

  constructor(state: PluginState, messages?: string[]) {
    this.state    = state;
    this.messages = messages;
  }

  static fromJSON(data: StatusJSON) {
    return new Status(data.state, data.messages);
  }

  isInvalid() {
    return this.state === "invalid";
  }
}

export class PluginInfo {
  readonly id: string;
  readonly about: About;
  readonly imageUrl?: string;
  readonly status: Status;
  readonly pluginFileLocation: string;
  readonly bundledPlugin: boolean;
  readonly extensions: Extension[];

  constructor(id: string,
              about: About,
              imageUrl: string | undefined,
              status: Status,
              pluginFileLocation: string,
              bundledPlugin: boolean,
              extensions: Extension[]) {
    this.id                 = id;
    this.about              = about;
    this.imageUrl           = imageUrl;
    this.status             = status;
    this.pluginFileLocation = pluginFileLocation;
    this.bundledPlugin      = bundledPlugin;
    this.extensions         = extensions;
  }

  static fromJSON(data: PluginInfoJSON): PluginInfo {
    const extensions: Extension[] = data.extensions.map((extension: ExtensionJSON) => Extension.fromJSON(extension));
    const links                   = data._links;
    const imageUrl                = (links && links.image && links.image.href);
    return new PluginInfo(data.id,
                          About.fromJSON(data.about || {} as AboutJSON),
                          imageUrl,
                          Status.fromJSON(data.status),
                          data.plugin_file_location,
                          data.bundled_plugin,
                          extensions);
  }

  supportsPluginSettings(): boolean {
    return this.firstExtensionWithPluginSettings() !== undefined;
  }

  types(): ExtensionType[] {
    return this.extensions.map((ext: Extension) => ext.type);
  }

  extensionOfType<T extends Extension>(type: ExtensionTypeString): T | undefined {
    return this.extensions.find((ext: Extension) => ext.type === type) as T;
  }

  firstExtensionWithPluginSettings() {
    return this.extensions.find((ext) => ext.hasPluginSettings());
  }

  hasErrors(): boolean {
    return this.status.state === "invalid";
  }

  getErrors(): string {
    if (this.hasErrors() && this.status.messages) {
      return _.join(this.status.messages, " ");
    }
    return "";
  }

  supportsStatusReport() {
    if (this.extensions) {
      const elasticAgentExtensionInfo = this.extensionOfType(ExtensionTypeString.ELASTIC_AGENTS);
      // @ts-ignore
      return elasticAgentExtensionInfo && elasticAgentExtensionInfo.capabilities && elasticAgentExtensionInfo.capabilities.supportsPluginStatusReport;
    }
    return false;
  }
}

export class PluginInfos extends Array<PluginInfo> {

  constructor(...pluginInfos: PluginInfo[]) {
    super(...pluginInfos);
    Object.setPrototypeOf(this, Object.create(PluginInfos.prototype));
  }

  static fromJSON(json: PluginInfosJSON) {
    return new PluginInfos(...json._embedded.plugin_info.map((pluginInfo) => PluginInfo.fromJSON(pluginInfo)));
  }

  optionsForDropdown() {
    return _.map(this, (pluginInfo: PluginInfo) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });
  }

  findByPluginId(pluginId: string) {
    return _.find(this, (pluginInfo) => pluginInfo.id === pluginId);
  }

  configRepoPluginsWhich(...caps: Array<keyof ConfigRepoCapabilities>): PluginInfos {
    return new PluginInfos(..._.filter(this, (pi) => {
      const kapable = _.get(
        pi.extensionOfType<ConfigRepoExtension>(ExtensionTypeString.CONFIG_REPO),
        "capabilities",
        {} as ConfigRepoCapabilities
      );
      return _.every(caps, (key) => (key in kapable) && !!kapable[key]);
    }));
  }

  getPluginInfosWithAuthorizeCapabilities(): PluginInfos {
    const filterFn = (value: PluginInfo) => {
      const authorizationSettings = value.extensionOfType(ExtensionTypeString.AUTHORIZATION) as AuthorizationExtension;
      return authorizationSettings && authorizationSettings.capabilities.canAuthorize;
    };

    const filteredPluginInfos = _.filter(this, filterFn) as unknown as PluginInfo[];
    return new PluginInfos(...filteredPluginInfos);
  }

  filterForExtension(type: ExtensionTypeString) {
    const filteredPluginInfos = _.filter(this, (pluginInfo: PluginInfo) => pluginInfo.extensionOfType(type) !== undefined);
    return new PluginInfos(...filteredPluginInfos);
  }
}
