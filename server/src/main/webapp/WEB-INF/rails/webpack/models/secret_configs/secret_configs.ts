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

import Stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {AutoSuggestions} from "models/roles/auto_suggestion";
import {Rules} from "models/rules/rules";
import {SecretConfigJSON, SecretConfigsJSON, SecretConfigsWithSuggestionsJSON} from "models/secret_configs/secret_configs_json";
import {Configurations} from "models/shared/configuration";

export class SecretConfig extends ValidatableMixin {
  id: Stream<string>;
  description: Stream<string>;
  pluginId: Stream<string>;
  properties: Stream<Configurations>;
  rules: Stream<Rules>;

  constructor(id: string,
              description: string,
              pluginId: string,
              properties: Configurations,
              rules: Rules,
              errors: Errors = new Errors()) {
    super();
    this.id          = Stream(id);
    this.description = Stream(description);
    this.pluginId    = Stream(pluginId);
    this.properties  = Stream(properties);
    this.rules       = Stream(rules);
    this.errors(errors);
    this.validatePresenceOf("id");
    this.validateIdFormat("id");
    this.validatePresenceOf("pluginId");
    this.validateEach("rules");
  }

  static fromJSON(data: SecretConfigJSON) {
    const errors         = new Errors(data.errors);
    const configurations = Configurations.fromJSON(data.properties);
    const rules          = Rules.fromJSON(data.rules);
    return new SecretConfig(data.id, data.description, data.plugin_id, configurations, rules, errors);
  }

  toJSON(): object {
    return {
      id:          this.id,
      description: this.description,
      plugin_id:   this.pluginId,
      properties:  this.properties,
      rules:       this.rules
    };
  }
}

export class SecretConfigs extends Array<Stream<SecretConfig>> {

  constructor(...secretConfigs: Array<Stream<SecretConfig>>) {
    super(...secretConfigs);
    Object.setPrototypeOf(this, Object.create(SecretConfigs.prototype));
  }

  static fromJSON(data: SecretConfigsJSON) {
    const secretConfigs = data
      ._embedded
      .secret_configs
      .map((secretConfigJson) => Stream(SecretConfig.fromJSON(secretConfigJson)));

    return new SecretConfigs(...secretConfigs);
  }
}

export class SecretConfigsWithSuggestions {
  secretConfigs: SecretConfigs;
  autoCompletion: AutoSuggestions;

  constructor(secretConfigs: SecretConfigs, autoCompletion: AutoSuggestions) {
    this.secretConfigs  = secretConfigs;
    this.autoCompletion = autoCompletion;
  }

  static fromJSON(data: SecretConfigsWithSuggestionsJSON): SecretConfigsWithSuggestions {
    return new SecretConfigsWithSuggestions(SecretConfigs.fromJSON(data), AutoSuggestions.fromJSON(data.auto_completion));
  }
}
