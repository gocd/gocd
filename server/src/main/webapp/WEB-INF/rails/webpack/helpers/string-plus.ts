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
import s from "underscore.string";

const functions = {
  defaultToIfBlank(value: string, defaultValue: string) {
    return s.isBlank(value) ? defaultValue : value;
  },

  snakeCaser(_key: string, value: any) {
    if (value && typeof value === "object" && !_.isArray(value)) {
      const replacement: { [key: string]: any } = {};
      for (const k in value) {
        if (!k.startsWith("__")) {
          if (Object.hasOwnProperty.call(value, k)) {
            replacement[_.snakeCase(k)] = value[k];
          }
        }
      }
      return replacement;
    }
    return value;
  },

  camelCaser(_key: string, value: any): any {
    if (value && typeof value === "object" && !_.isArray(value)) {
      const replacement: { [key: string]: any } = {};
      for (const k in value) {
        if (Object.hasOwnProperty.call(value, k)) {
          replacement[_.camelCase(k)] = value[k];
        }
      }
      return replacement;
    }
    return value;
  },

  terminateWithPeriod(str: string) {
    if (s.endsWith(str, ".")) {
      return str;
    } else {
      return `${str}.`;
    }
  },

  caseInsensitiveCompare(a: string, b: string) {
    a = a.toLocaleLowerCase();
    b = b.toLocaleLowerCase();

    return (a < b) ? -1 : (a > b) ? 1 : 0; // eslint-disable-line no-nested-ternary
  }
};

export const mixins = _.assign({}, s, functions);
