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
import {ConfigValue, EncryptedValue, PlainTextValue} from "models/shared/config_value";

export interface PropertyJSON {
  key: string;
  value?: string | null | undefined;
  encrypted_value?: string | null | undefined;
  errors?: { [key: string]: string[] };
}

export class Configurations {
  private readonly configurations: Configuration[];

  constructor(configurations: Configuration[]) {
    this.configurations = configurations;
  }

  public static fromJSON(properties: PropertyJSON[]): Configurations {
    return new Configurations(properties.map((property) => Configuration.fromJSON(property)));
  }

  public valueFor(key: string): string | undefined {
    const config = this.findConfiguration(key);
    if (config) {
      return config.getValue();
    }
  }

  public setConfiguration(key: string, value: string): void {
    const existingConfig = this.findConfiguration(key);
    if (!existingConfig) {
      this.configurations.push(new Configuration(key, new PlainTextValue(value)));
    } else {
      existingConfig.updateValue(value);
    }
  }

  public findConfiguration(key: string): Configuration | undefined {
    return _.find(this.configurations, ["key", key]);
  }

  public asMap(): Map<string, string> {
    return new Map<string, string>(this.configurations.map((config) => [config.key, config.displayValue()] as [string, string]));
  }

  public allConfigurations(): Configuration[] {
    return this.configurations;
  }

  public count(): number {
    return this.configurations.length;
  }

  public toJSON() {
    return this.configurations;
  }

  public asString(): string {
    const configValues = this.configurations
                             .map((config) => `${config.key}=${config.displayValue()}`)
                             .join(',');
    return `[${configValues}]`;
  }
}

export class Configuration {
  readonly key: string;
  readonly errors?: string[];
  private value: ConfigValue;

  constructor(key: string, value: EncryptedValue | PlainTextValue, errors?: string[]) {
    this.key    = key;
    this.value  = value;
    this.errors = errors;
  }

  static fromJSON(config: PropertyJSON): Configuration {
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

  public displayValue(): string {
    return this.value.getDisplayValue();
  }

  public getValue(): string {
    return this.value.getValue();
  }

  public isEncrypted(): boolean {
    return this.value.isEncrypted();
  }

  public updateValue(value: string) {
    if (value !== this.getValue()) {
      this.value = new PlainTextValue(value);
    }
  }

  public toJSON(): object {
    if (this.isEncrypted()) {
      return {key: this.key, encrypted_value: this.getValue()};
    }

    return {key: this.key, value: this.getValue()};
  }
}
