/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define([
  'lodash', 'models/pipeline_configs/pluggable_tasks', 'models/pipeline_configs/plugin_infos', 'models/pipeline_configs/tasks'
], function (_, PluggableTasks, PluginInfos, Tasks) {
  describe('PluggableTasks', function () {
    describe('init', function () {
      it('should build pluggable task types from plugins', function () {
        var dockerTaskPlugin = new PluginInfos.PluginInfo({
          id:           'docker.task',
          type:         'task',
          display_name: 'Docker', //eslint-disable-line camelcase
          description:  'Docker task plugin'
        });

        var scmPlugin = new PluginInfos.PluginInfo({
          id:          'github.pr',
          type:        'scm',
          description: 'Github PR Plugin'
        });

        var xunitConvertor = new PluginInfos.PluginInfo({
          id:           'xunitConvertor',
          type:         'task',
          display_name: 'xUnit', //eslint-disable-line camelcase
          description:  'Xunit Convertor'
        });

        PluginInfos([dockerTaskPlugin, scmPlugin, xunitConvertor]);

        PluggableTasks.init();

        expect(_.size(PluggableTasks.Types)).toBe(2);
        expect(_.keys(PluggableTasks.Types)).toEqual(['docker.task', 'xunitConvertor']);
        expect(_.values(PluggableTasks.Types)).toEqual([{type: Tasks.Task.PluginTask, description: 'Docker'},
          {type: Tasks.Task.PluginTask, description: 'xUnit'}]);
      });
    });
  });
});
