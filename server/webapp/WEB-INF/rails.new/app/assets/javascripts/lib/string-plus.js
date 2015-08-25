/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(['string', 'lodash'], function (s, _) {
  var mixins = {
    defaultToIfBlank: function (value, defaultValue) {
      return s.isBlank(value) ? defaultValue : value;
    },
    overrideToJSON:   function (prop) {
      if (prop && prop() && prop().toJSON) {
        prop.toJSON = prop().toJSON;
      }
      return prop;
    },
    snakeCaser:       function (key, value) {
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
