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

export interface ConfigValue {
  getValue(): string;

  setValue(value: string): void;

  isEncrypted(): boolean;
}

export class EncryptedValue implements ConfigValue {
  private val: string;

  constructor(val: string) {
    this.val = val;
  }

  getValue(): string {
    return this.val;
  }

  setValue(value: string): void {
    this.val = value;
  }

  isEncrypted(): boolean {
    return true;
  }
}

export class PlainTextValue implements ConfigValue {
  private val: string;

  constructor(val: string) {
    this.val = val;
  }

  getValue(): string {
    return this.val;
  }

  setValue(value: string): void {
    this.val = value;
  }

  isEncrypted(): boolean {
    return false;
  }

}

export class Configuration {
  readonly key: string;
  readonly errors?: string[];
  private _value: ConfigValue;

  constructor(key: string, value: EncryptedValue | PlainTextValue, errors?: string[]) {
    this.key    = key;
    this._value = value;
    this.errors = errors;
  }

  static fromJSON(config: any): Configuration {
    let errors;
    if (config.errors) {
      errors = config.errors[config.key];
    }
    let value;
    if (config.encrypted_value) {
      value = new EncryptedValue(config.encrypted_value);
    } else {
      value = new PlainTextValue(config.value || "");
    }
    return new Configuration(config.key, value, errors);
  }

  get value(): string {
    if (this._value) {
      return this._value.getValue();
    } else {
      return "";
    }
  }

  set value(val: string) {
    if (val === this._value.getValue()) {
      return;
    }
    this._value = new PlainTextValue(val);
  }

  toJSON() {
    if (this._value.isEncrypted()) {
      return {
        key: this.key,
        encrypted_value: this.value
      };
    } else {
      return {
        key: this.key,
        value: this.value
      };
    }
  }
}

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
