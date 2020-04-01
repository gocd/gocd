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
import {Configuration} from "models/shared/configuration";
import {PlainTextValue} from "models/shared/config_value";

export class PluginSettings {
  public static readonly API_VERSION: string = "v1";
  readonly plugin_id: string;
  configuration: Configuration[];

  constructor(plugin_id: string, configuration?: Configuration[]) {
    this.plugin_id     = plugin_id;
    this.configuration = configuration || [];
  }

  static fromJSON(json: any): PluginSettings {
    return new PluginSettings(json.plugin_id, json.configuration.map((config: any) => Configuration.fromJSON(config)));
  }

  findConfiguration(key: string): Configuration | undefined {
    return _.find(this.configuration, (config) => config.key === key);
  }

  valueFor(key: string) {
    const config = this.findConfiguration(key);
    return config ? config.value : undefined;
  }

  setConfiguration(key: string, value: string) {
    const config = this.findConfiguration(key);
    if (config) {
      config.value = value;
    } else {
      this.configuration.push(new Configuration(key, new PlainTextValue(value)));
    }
  }

  toJSON(): any {
    return {
      plugin_id: this.plugin_id,
      configuration: this.configuration.map((config) => config.toJSON())
    };
  }
}
