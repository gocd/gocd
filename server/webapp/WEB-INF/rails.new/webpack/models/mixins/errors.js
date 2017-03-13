/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const _      = require('lodash');
const s      = require('string-plus');
const Errors = function(errors = {}) {
  this.add = (attrName, message) => {
    errors[attrName] = errors[attrName] || [];
    errors[attrName].push(message);
  };

  this.clear = (optionalAttribute) => {
    optionalAttribute ? (errors[optionalAttribute] = []) : (errors = {});
  };

  this.errors = (optionalAttribute) => optionalAttribute ? errors[optionalAttribute] : errors;

  this.hasErrors = (attr) => !_.isEmpty(errors[attr]);

  this._isEmpty = () => _.isEmpty(errors);

  this.errorsForDisplay = (attrName) => _.map(errors[attrName] || [], s.terminateWithPeriod).join(" ");
};

module.exports = Errors;
