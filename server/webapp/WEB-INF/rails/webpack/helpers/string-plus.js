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

const s                = require('string');
const _                = require('lodash');
const Stream           = require('mithril/stream');
const POSITIVE_INTEGER = /^\d+$/;

const mixins = {
  defaultToIfBlank(value, defaultValue) {
    return s.isBlank(value) ? defaultValue : value;
  },

  defaultToStream(param, defaultValue) {
    return typeof param === 'function' ? param : Stream(typeof param === 'undefined' ? defaultValue : param);
  },

  collectionToJSON(prop) {
    if (prop && prop() && prop().toJSON) {
      prop.toJSON = prop().toJSON;
    }
    return prop;
  },

  withNewJSONImpl(prop, jsonFunc) {
    prop.toJSON = () => jsonFunc(prop());

    return prop;
  },

  stringToArray(string) {
    if (_.isArray(string)) {
      return string;
    } else if (s.isBlank(string)) {
      return [];
    } else {
      return _.chain(string.split(',')).map(_.trim).filter((thing) => !s.isBlank(thing)).value();
    }
  },

  isPositiveInteger(value) {
    return POSITIVE_INTEGER.test(String(value).trim());
  },

  snakeCaser(_key, value) {
    if (value && typeof value === 'object' && !_.isArray(value)) {
      const replacement = {};
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

  camelCaser(_key, value) {
    if (value && typeof value === 'object' && !_.isArray(value)) {
      const replacement = {};
      for (const k in value) {
        if (Object.hasOwnProperty.call(value, k)) {
          replacement[_.camelCase(k)] = value[k];
        }
      }
      return replacement;
    }
    return value;
  },

  terminateWithPeriod(str) {
    if (s.endsWith(str, '.')) {
      return str;
    } else {
      return `${str}.`;
    }
  },

  caseInsensitiveCompare(a, b) {
    a = a.toLocaleLowerCase();
    b = b.toLocaleLowerCase();

    return (a < b) ? -1 : (a > b) ? 1 : 0; // eslint-disable-line no-nested-ternary
  }
};

module.exports = _.mixin(s, mixins);
