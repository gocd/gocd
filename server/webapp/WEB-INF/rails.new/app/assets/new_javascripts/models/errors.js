/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(['mithril', 'lodash'], function (m, _) {
  var Errors = function (errors) {
    errors = errors || {};

    this.add = function (attrName, message) {
      errors[attrName] = errors[attrName] || [];
      errors[attrName].push(message);
    };

    this.clear = function (optionalAttribute) {
      optionalAttribute ? (errors[optionalAttribute] = []) : (errors = {});
    };

    this.errors = function (optionalAttribute) {
      return optionalAttribute ? errors[optionalAttribute] : errors;
    };

    this.hasErrors = function (attr) {
      return !_.isEmpty(errors[attr]);
    };

    this._isEmpty = function () {
      return _.isEmpty(errors);
    };

    this.errorsForDisplay = function (attrName) {
      return _.map(errors[attrName] || [], function (message) {
        return message + ".";
      }).join(" ");
    };
  };

  return Errors;
});
