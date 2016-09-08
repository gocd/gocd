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

define([
  'lodash', 'models/pipeline_configs/pluggable_scms', 'models/pipeline_configs/plugin_infos', 'models/pipeline_configs/materials'
], function (_, PluggableSCMs, PluginInfos, Materials) {
  describe('PluggableTasks', function () {
    describe('init', function () {
      it('should build pluggable scm types from plugins', function () {
        var dockerTaskPlugin = new PluginInfos.PluginInfo({
          id:          'docker.task',
          type:        'task',
          description: 'Docker task plugin'
        });

        var scmPlugin = new PluginInfos.PluginInfo({
          id:           'github.pr',
          type:         'scm',
          display_name: 'Github PR', // eslint-disable-line camelcase
          description:  'Github PR Plugin'
        });

        var xunitConvertor = new PluginInfos.PluginInfo({
          id:           'scm.poller',
          type:         'scm',
          display_name: 'SCM Poller', // eslint-disable-line camelcase
          description:  'SCM Poller'
        });

        PluginInfos([dockerTaskPlugin, scmPlugin, xunitConvertor]);

        PluggableSCMs.init();

        expect(_.size(PluggableSCMs.Types)).toBe(2);
        expect(_.keys(PluggableSCMs.Types)).toEqual(['github.pr', 'scm.poller']);
        expect(_.values(PluggableSCMs.Types)).toEqual([{
          type:        Materials.Material.PluggableMaterial,
          description: 'Github PR'
        },
          {type: Materials.Material.PluggableMaterial, description: 'SCM Poller'}]);
      });
    });
  });
});
