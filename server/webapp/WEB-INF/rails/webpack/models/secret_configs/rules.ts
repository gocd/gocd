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
import * as stream from "mithril/stream";
import {Errors, ErrorsJSON} from "models/mixins/errors";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {DynamicSuggestionProvider} from "models/secret_configs/suggestion_provider";

export interface RuleJSON {
  directive: string;
  action: string;
  type: string;
  resource: string;
  errors?: ErrorsJSON;
}

export class Rule extends ValidatableMixin {
  directive: Stream<string>;
  action: Stream<string>;
  type: Stream<string>;
  resource: Stream<string>;
  private __provider!: DynamicSuggestionProvider;

  constructor(directive: string, action: string, type: string, resource: string, errors: Errors = new Errors()) {
    super();
    ValidatableMixin.call(this);
    this.directive = stream(directive);
    this.action    = stream(action);
    this.type      = stream(type);
    this.resource  = stream(resource);
    this.errors(errors);
    this.validatePresenceOf("directive");
    this.validatePresenceOf("action");
    this.validatePresenceOf("type");
  }

  static fromJSON(ruleJSON: RuleJSON) {
    const errors = new Errors(ruleJSON.errors);
    return new Rule(ruleJSON.directive, ruleJSON.action, ruleJSON.type, ruleJSON.resource, errors);
  }

  setProvider(autoCompleteHelper: Map<string, string[]>) {
    this.__provider = new DynamicSuggestionProvider(this.type(), autoCompleteHelper);
  }

  getProvider() {
    return this.__provider;
  }

  updateProvider() {
    this.__provider.setType(this.type());
    this.__provider.update();
  }

  toJSON(): object {
    return {
      directive: this.directive,
      action: this.action,
      type: this.type,
      resource: this.resource
    };
  }
}

export class Rules extends Array<Stream<Rule>> {
  constructor(...rules: Array<Stream<Rule>>) {
    super(...rules);
    Object.setPrototypeOf(this, Object.create(Rules.prototype));
  }

  static fromJSON(rulesJSON: RuleJSON[]) {
    if (!rulesJSON) {
      return [];
    }
    return new Rules(...rulesJSON.map((rule) => stream(Rule.fromJSON(rule))));
  }
}
