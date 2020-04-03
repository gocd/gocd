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

import Stream from "mithril/stream";
import {Errors, ErrorsJSON} from "models/mixins/errors";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

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

  constructor(directive: string, action: string, type: string, resource: string, errors: Errors = new Errors()) {
    super();
    this.directive = Stream(directive);
    this.action    = Stream(action);
    this.type      = Stream(type);
    this.resource  = Stream(resource);
    this.errors(errors);
    this.validatePresenceOf("directive");
    this.validatePresenceOf("action");
    this.validatePresenceOf("type");
    this.validatePresenceOf("resource");
  }

  static fromJSON(ruleJSON: RuleJSON) {
    const errors = new Errors(ruleJSON.errors);
    return new Rule(ruleJSON.directive, ruleJSON.action, ruleJSON.type, ruleJSON.resource, errors);
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
    return new Rules(...rulesJSON.map((rule) => Stream(Rule.fromJSON(rule))));
  }
}
