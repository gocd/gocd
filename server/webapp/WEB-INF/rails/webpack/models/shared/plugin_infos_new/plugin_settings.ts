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

export class Configuration {
  readonly key: string;
           value?: string;
  readonly errors?: string[];

  constructor(key: string, value?: string, errors?: string[]) {
    this.key    = key;
    this.value  = value;
    this.errors = errors;
  }

  static fromJSON(config: any): Configuration {
    let errors;
    if (config.errors) {
      errors = config.errors[config.key];
    }
    return new Configuration(config.key, config.value, errors);
  }
}

export class PluginSettings {
  readonly pluginId: string;
           configuration: Configuration[];

  constructor(pluginId: string, configuration: Configuration[]) {
    this.pluginId      = pluginId;
    this.configuration = configuration;
  }

  static fromJSON(json: any): PluginSettings {
    return new PluginSettings(json.data.plugin_id, json.data.configuration.map((config: any) => Configuration.fromJSON(config)));
  }
}
