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

import * as Routes from "gen/ts-routes";
import {About} from "./about";
import {ExtensionType} from "./extension_type";
import {Extension} from "./extensions";

const AjaxHelper = require("helpers/ajax_helper");

enum State {
  active  = "active",
  invalid = "invalid"
}

class Status {
  readonly state: State;
  readonly messages?: string[];

  constructor(state: State, messages?: string[]) {
    this.state    = state;
    this.messages = messages;
  }

  static fromJSON(data: any) {
    return new Status(data.state, data.messages);
  }
}

export class PluginInfo<T extends Extension> {
  readonly id: string;
  readonly about: About;
  readonly imageUrl: string;
  readonly status: Status;
  readonly pluginFileLocation: string;
  readonly bundledPlugin: boolean;
  readonly extensions: T[];

  constructor(id: string, about: About, imageUrl: string, status: Status, pluginFileLocation: string, bundledPlugin: boolean,
              extensions: T[]) {
    this.id                 = id;
    this.about              = about;
    this.imageUrl           = imageUrl;
    this.status             = status;
    this.pluginFileLocation = pluginFileLocation;
    this.bundledPlugin      = bundledPlugin;
    this.extensions         = extensions;
  }

  static fromJSON(data: any, links?: any) {
    const extensions: Extension[] = data.extensions.map((extension: any) => Extension.fromJSON(extension));
    const imageUrl                = (links && links.image && links.image.href) || "";
    return new PluginInfo(data.id, About.fromJSON(data.about), imageUrl, Status.fromJSON(data.status),
      data.plugin_file_location, data.bundled_plugin, extensions);
  }

  supportsPluginSettings(): boolean {
    return this.extensions.length > 0 && this.extensions.reduce((previous, ext) => previous && ext.hasPluginSettings(), true);
  }

  types(): ExtensionType[] {
    return this.extensions.map((ext: Extension) => ext.type);
  }

  extensionOfType(type: ExtensionType): T | undefined {
    return this.extensions.find((ext: T) => ext.type === type);
  }

  firstExtensionWithPluginSettings(): T | undefined {
    return this.extensions.find((ext) => ext.hasPluginSettings());
  }
}

export class PluginInfos {

  public static readonly API_VERSION: string = "v4";
  readonly pluginInfo: Array<PluginInfo<Extension>>;

  constructor(pluginInfo: Array<PluginInfo<Extension>>) {
    this.pluginInfo = pluginInfo;
  }

  static all() {
    return AjaxHelper.GET({
      url: Routes.apiv4AdminPluginInfoIndexPath(),
      apiVersion: PluginInfos.API_VERSION,
      type: PluginInfos
    });
  }

  static fromJSON(data: any) {
    return data._embedded.plugin_info.map((pluginInfo: any) => PluginInfo.fromJSON(pluginInfo));
  }
}
