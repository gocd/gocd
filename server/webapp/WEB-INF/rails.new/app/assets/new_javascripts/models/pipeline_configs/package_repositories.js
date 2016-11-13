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


define(['mithril', 'lodash', 'jquery', 'models/pipeline_configs/materials', 'models/pipeline_configs/plugin_infos'],
  function (m, _, $, Materials, PluginInfos) {
    var PackageRepositories = {};

    PackageRepositories.init = function () {
      if (!_.isEmpty(PluginInfos.filterByType('package-repository'))) {
        PackageRepositories.Types['package'] = {
          type:        Materials.Material.PackageMaterial,
          description: 'Package Material'
        };
      }
    };

    PackageRepositories.Types = {};

    return PackageRepositories;
  });
