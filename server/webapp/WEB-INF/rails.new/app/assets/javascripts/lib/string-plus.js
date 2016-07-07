/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define(['string', 'lodash', 'mithril'], function (s, _, m) {
  var POSITIVE_INTEGER = /^\d+$/;

  var mixins = {
    defaultToIfBlank: function (value, defaultValue) {
      return s.isBlank(value) ? defaultValue : value;
    },

    coerceToMprop: function (param, defaultValue) {
      return typeof param === 'function' ? param : m.prop(typeof param === 'undefined' ? defaultValue : param);
    },

    collectionToJSON: function (prop) {
      if (prop && prop() && prop().toJSON) {
        prop.toJSON = prop().toJSON;
      }
      return prop;
    },

    withNewJSONImpl: function (prop, jsonFunc) {
      prop.toJSON = function () {
        return jsonFunc(prop());
      };

      return prop;
    },

    stringToArray: function (string) {
      if (_.isArray(string)) {
        return string;
      } else if (s.isBlank(string)) {
        return [];
      } else {
        return _.chain(string.split(',')).map(_.trim).filter(function (thing) {
          return !s.isBlank(thing);
        }).value();
      }
    },

    isPositiveInteger: function (value) {
      return POSITIVE_INTEGER.test(String(value).trim())
    },

    snakeCaser: function (key, value) {
      if (value && typeof value === 'object' && !_.isArray(value)) {
        var replacement = {};
        for (var k in value) {
          if (Object.hasOwnProperty.call(value, k)) {
            replacement[_.snakeCase(k)] = value[k];
          }
        }
        return replacement;
      }
      return value;
    },

    uuid: function guid() {
      function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
          .toString(16)
          .substring(1);
      }

      return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
    }
  };

  return _.mixin(s, mixins);
});
