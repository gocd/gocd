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
  'lodash', 'models/pipeline_configs/package_repositories', 'models/pipeline_configs/plugin_infos', 'models/pipeline_configs/materials'
], function (_, PackageRepositories, PluginInfos, Materials) {
  describe('Package Repositories', function () {
    describe('init', function () {
      afterEach(function () {
        PackageRepositories.Types = {};
      });
      it('should build package material type from plugins', function () {
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

        var debPlugin = new PluginInfos.PluginInfo({
          id:          'deb',
          type:        'package-repository',
          name:        'Debian', // eslint-disable-line camelcase
          description: 'Debian'
        });

        PluginInfos([dockerTaskPlugin, scmPlugin, debPlugin]);

        PackageRepositories.init();

        expect(_.keys(PackageRepositories.Types)).toEqual(['package']);
        expect(_.values(PackageRepositories.Types)).toEqual([{
          type:        Materials.Material.PackageMaterial,
          description: 'Package Material'
        }]);
      });

      it('should not build package material type if no plugin exists', function() {
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

        PluginInfos([dockerTaskPlugin, scmPlugin]);

        PackageRepositories.init();

        expect(PackageRepositories.Types).toEqual({});

      });

    });
  });
});
