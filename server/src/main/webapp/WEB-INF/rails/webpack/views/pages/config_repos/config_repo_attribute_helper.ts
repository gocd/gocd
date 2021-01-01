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
import m from "mithril";
import {ConfigRepo, humanizedMaterialAttributeName} from "models/config_repos/types";
import {USER_NS_PREFIX} from "models/mixins/configuration_properties";
import {Configuration} from "models/shared/configuration";
import {Lock} from "views/components/icons";

export function resolveHumanReadableAttributes(obj: object) {
  const attrs  = new Map();
  const keys   = Object.keys(obj).map(humanizedMaterialAttributeName);
  const values = Object.values(obj);

  keys.forEach((key, index) => attrs.set(key, values[index]));
  return attrs;
}

export function allAttributes(repo: ConfigRepo): Map<string, string> {
  const initial            = new Map([["Type", repo.material().type()!]]);
  const filteredAttributes = _.reduce(repo.material().attributes(),
    resolveKeyValueForAttribute,
    initial);
  return _.reduce(repo.knownProps(),
    (accumulator, value) => resolveKeyValueForAttribute(accumulator, value.value, value.key),
    filteredAttributes
  );
}

export function userDefinedProperties(repo: ConfigRepo): Map<string, m.Children> {
  return _.reduce(repo.userProps() as Configuration[], (memo, prop) => {
    memo.set(prop.key.substring(USER_NS_PREFIX.length), displayValue(prop));
    return memo;
  }, new Map<string, m.Children>());
}

function displayValue(prop: Configuration): m.Children {
  return prop.encrypted ? [m(Lock, { iconOnly: true }), " ", prop.displayValue()] : prop.displayValue();
}

function resolveKeyValueForAttribute(accumulator: Map<string, string>, value: any, key: string) {
  if (key.startsWith("__") || ["autoUpdate", "name", "destination"].includes(key)) {
    return accumulator;
  }

  let renderedValue = value;

  const renderedKey = humanizedMaterialAttributeName(key);

  // test for value being a stream
  if (_.isFunction(value)) {
    value = value();
  }

  // test for value being an EncryptedPassword
  if (value && value.valueForDisplay) {
    renderedValue = value.valueForDisplay();
  }
  accumulator.set(renderedKey, _.isFunction(renderedValue) ? renderedValue() : renderedValue);
  return accumulator;
}
