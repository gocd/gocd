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

var _              = require('lodash');
var Tasks          = require('models/pipeline_configs/tasks');
var PluginInfos    = require('models/pipeline_configs/plugin_infos');
var PluggableTasks = {};

PluggableTasks.init = function () {
  _.each(PluginInfos.filterByType('task'), function (pluginInfo) {
    PluggableTasks.Types[pluginInfo.id()] = {
      type:        Tasks.Task.PluginTask,
      description: pluginInfo.displayName()
    };
  });
};

PluggableTasks.Types = {};

module.exports = PluggableTasks;
