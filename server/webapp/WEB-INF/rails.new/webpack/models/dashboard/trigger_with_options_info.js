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

const Stream               = require('mithril/stream');
const _                    = require('lodash');
const s                    = require('helpers/string-plus');
const sparkRoutes          = require('helpers/spark_routes');
const AjaxHelper           = require('helpers/ajax_helper');
const EnvironmentVariables = require('models/dashboard/environment_variables');

const TriggerWithOptionsInfo = function (materials, plainTextVariables, secureVariables) {
  this.materials          = materials;
  this.plainTextVariables = plainTextVariables;
  this.secureVariables    = secureVariables;
};

const isSecure    = (v) => v.secure;
const isPlainText = (v) => !v.secure;

TriggerWithOptionsInfo.fromJSON = (json) => {
  const materials = JSON.parse(JSON.stringify(json.materials, s.camelCaser));

  _.each(materials, (material) => {
    material.selection = Stream();
  });

  const plainTextVariables = EnvironmentVariables.fromJSON(_.filter(json.variables, isPlainText));
  const secureVariables    = EnvironmentVariables.fromJSON(_.filter(json.variables, isSecure));

  return new TriggerWithOptionsInfo(materials, plainTextVariables, secureVariables);
};

TriggerWithOptionsInfo.all = function (pipelineName) {
  return AjaxHelper.GET({
    url:        sparkRoutes.pipelineTriggerWithOptionsViewPath(pipelineName),
    type:       TriggerWithOptionsInfo,
    apiVersion: 'v1',
  });
};

module.exports = TriggerWithOptionsInfo;
