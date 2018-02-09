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

const _ = require('lodash');

const Materials            = require('models/dashboard/materials');
const EnvironmentVariables = require('models/dashboard/environment_variables');

const TriggerWithOptionsInfo = function (materials, plainTextVariables, secureVariables) {
  this.materials          = materials;
  this.plainTextVariables = plainTextVariables;
  this.secureVariables    = secureVariables;
};

const isSecure    = (v) => v.secure;
const isPlainText = (v) => !v.secure;

TriggerWithOptionsInfo.fromJSON = (json) => {
  const materials                     = Materials.fromJSON(json.materials);
  const plainTextEnvironmentVariables = EnvironmentVariables.fromJSON(_.filter(json.variables, isPlainText));
  const secureEnvironmentVariables    = EnvironmentVariables.fromJSON(_.filter(json.variables, isSecure));

  return new TriggerWithOptionsInfo(materials, plainTextEnvironmentVariables, secureEnvironmentVariables);
};

module.exports = TriggerWithOptionsInfo;
