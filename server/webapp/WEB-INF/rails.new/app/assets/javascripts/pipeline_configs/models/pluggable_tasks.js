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

define(['mithril', 'lodash', './tasks', './plugins'], function (m, _, Tasks, Plugins) {
  var PluggableTasks = {};

  PluggableTasks.init = function () {
    _.each(Plugins.filterByType('task'), function (plugin) {
      PluggableTasks.Types[plugin.id()] = {
        type:        Tasks.Task.PluginTask,
        description: plugin.name()
      };
    });
  };

  PluggableTasks.Types = {};

  return PluggableTasks;
});
