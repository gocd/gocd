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

xdescribe("Pluggable SCM Widget", function () {
  var $ = require("jquery");
  var m = require("mithril");

  var Materials          = require("models/pipeline_configs/materials");
  var SCMs               = require("models/pipeline_configs/scms");
  var PluggableSCMWidget = require("views/pipeline_configs/pluggable_scm_widget");
  var PluginInfos        = require("models/pipeline_configs/plugin_infos");

  var $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  afterEach(function () {
    SCMs([]);
    SCMs.scmIdToEtag = {};
  });


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

    beforeEach(function () {
      spyOn(PluginInfos.PluginInfo, 'get').and.returnValue($.Deferred().promise());
      spyOn(angular, 'bootstrap');
      mount(pluggableMaterial);
    });

    afterEach(function () {
      unmount();
    });

    it('should bind name', function () {
      expect($root.find(".pluggable-scm li:nth-child(1)>label")).toHaveText('Name');
      expect($root.find(".pluggable-scm li:nth-child(1)>span")).toHaveText(github.name());
    });

    it('should bind auto update field', function () {
      expect($root.find(".pluggable-scm li:nth-child(2)>label")).toHaveText('AutoUpdate');
      expect(Boolean($root.find(".pluggable-scm li:nth-child(2)>span").text())).toBe(github.autoUpdate());
    });

    it('should bind url configuration', function () {
      expect($root.find(".pluggable-scm li:nth-child(3)>label")).toHaveText('Url');
      expect($root.find(".pluggable-scm li:nth-child(3)>span")).toHaveText('path/to/repo');
    });

    it('should bind username configuration', function () {
      expect($root.find(".pluggable-scm li:nth-child(4)>label")).toHaveText('Username');
      expect($root.find(".pluggable-scm li:nth-child(4)>span")).toHaveText('some_name');
    });

    it('should bind the ignore fields', function () {
      expect($root.find("input[data-prop-name='ignore']")).toHaveValue('*.doc');
    });

    it('should bind destination', function () {
      expect($root.find("input[data-prop-name='destination']")).toHaveValue(pluggableMaterial.destination());
    });
  });

  describe('NewView', function () {
    beforeEach(function () {
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

      spyOn(PluginInfos.PluginInfo, 'get').and.returnValue($.Deferred().promise());
      spyOn(angular, 'bootstrap');
    });

    afterEach(function () {
      SCMs([]);
    });

    it('should have a dropdown to select a scm', function () {
      mount(Materials.create({
        type:       'plugin',
        pluginInfo: new PluginInfos.PluginInfo({id: 'github.pr', version: '1.0.0'})
      }));

      expect($('.scm-selector select option')).toHaveLength(1);
      expect($('.scm-selector select option')).toHaveText('material_1');
      expect($('.scm-selector select option')).toHaveValue('plugin_id_1');

      unmount();
    });

    it('should not have a dropdown in absence of matching scms', function () {
      mount(Materials.create({
        type:       'plugin',
        pluginInfo: new PluginInfos.PluginInfo({id: 'unkown', version: '1.0.0'})
      }));

      expect($('.scm-selector select option')).toHaveLength(0);

      unmount();
    });
  });

  function mount(pluggableMaterial) {
    m.mount(root, {
      view: function () {
        return m(PluggableSCMWidget, {material: pluggableMaterial});
      }
    });
    m.redraw();
  }

  var unmount = function () {
    m.mount(root, null);
    m.redraw();
  };

});
