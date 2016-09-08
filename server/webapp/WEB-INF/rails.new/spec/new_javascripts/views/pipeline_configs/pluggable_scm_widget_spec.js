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

define(["jquery", "mithril", "models/pipeline_configs/materials", "models/pipeline_configs/scms", "views/pipeline_configs/pluggable_scm_widget",
  "models/pipeline_configs/plugin_infos"],
  function ($, m, Materials, SCMs, PluggableSCMWidget, PluginInfos) {
    describe("Pluggable SCM Widget", function () {

      var $root = $('#mithril-mount-point'), root = $root.get(0);

      describe('EditView', function () {
        /* eslint-disable camelcase */
        var github = new SCMs.SCM({
          id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
          name:            'Github PR',
          auto_update:     true,
          plugin_metadata: {id: 'github.pr', version: '1.1'},
          configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
        });
        /* eslint-enable camelcase */

        var pluggableMaterial = Materials.create({
          type:        'plugin',
          scm:         github,
          filter:      new Materials.Filter({ignore: ['*.doc']}),
          destination: 'dest_folder'
        });

        beforeAll(function () {
          spyOn(PluginInfos.PluginInfo, 'byId').and.returnValue($.Deferred().promise());
          spyOn(angular, 'bootstrap');
          mount(pluggableMaterial);
        });

        afterAll(function () {
          m.mount(root, null);
        });

        it('should bind name', function () {
          expect($root.find(".pluggable-scm li:nth-child(1)>label").text()).toBe('Name');
          expect($root.find(".pluggable-scm li:nth-child(1)>span").text()).toBe(github.name());
        });

        it('should bind auto update field', function () {
          expect($root.find(".pluggable-scm li:nth-child(2)>label").text()).toBe('AutoUpdate');
          expect(Boolean($root.find(".pluggable-scm li:nth-child(2)>span").text())).toBe(github.autoUpdate());
        });

        it('should bind url configuration', function () {
          expect($root.find(".pluggable-scm li:nth-child(3)>label").text()).toBe('Url');
          expect($root.find(".pluggable-scm li:nth-child(3)>span").text()).toBe('path/to/repo');
        });

        it('should bind username configuration', function () {
          expect($root.find(".pluggable-scm li:nth-child(4)>label").text()).toBe('Username');
          expect($root.find(".pluggable-scm li:nth-child(4)>span").text()).toBe('some_name');
        });

        it('should bind the ignore fields', function () {
          expect($root.find("input[data-prop-name='ignore']").val()).toBe('*.doc');
        });

        it('should bind destination', function () {
          expect($root.find("input[data-prop-name='destination']").val()).toBe(pluggableMaterial.destination());
        });
      });

      describe('NewView', function () {
        beforeAll(function () {
          SCMs([
            new SCMs.SCM({
              id:              'plugin_id_1',
              name:            'material_1',
              plugin_metadata: {id: 'github.pr', version: '1.1'} // eslint-disable-line camelcase

            }),
            new SCMs.SCM({
              id:              'plugin_id_2',
              name:            'material_2',
              plugin_metadata: {id: 'scm_plugin', version: '1.1'} // eslint-disable-line camelcase
            })
          ]);

          spyOn(PluginInfos.PluginInfo, 'byId').and.returnValue($.Deferred().promise());
          spyOn(angular, 'bootstrap');
        });

        afterAll(function() {
          SCMs([]);
        });

        it('should have a dropdown to select a scm', function () {
          mount(Materials.create({
            type:        'plugin',
            pluginInfo:  new PluginInfos.PluginInfo({id: 'github.pr'})
          }));

          expect($('.scm-selector select option').length).toBe(1);
          expect($('.scm-selector select option').text()).toBe('material_1');
          expect($('.scm-selector select option').val()).toBe('plugin_id_1');
        });

        it('should not have a dropdown in absence of matching scms', function () {
          mount(Materials.create({
            type:        'plugin',
            pluginInfo:  new PluginInfos.PluginInfo({id: 'unkown'})
          }));

          expect($('.scm-selector select option').length).toBe(0);
        });
      });

      function mount(pluggableMaterial) {
        m.mount(root,
          m.component(PluggableSCMWidget, {material: pluggableMaterial})
        );
        m.redraw(true);
      }
    });
  });
