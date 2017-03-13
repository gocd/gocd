/*
 * Copyright 2017 ThoughtWorks, Inc.
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

xdescribe("Pluggable SCM Widget", () => {
  const $ = require("jquery");
  const m = require("mithril");

  const Materials          = require("models/pipeline_configs/materials");
  const SCMs               = require("models/pipeline_configs/scms");
  const PluggableSCMWidget = require("views/pipeline_configs/pluggable_scm_widget");
  const PluginInfos        = require("models/pipeline_configs/plugin_infos");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  afterEach(() => {
    SCMs([]);
    SCMs.scmIdToEtag = {};
  });


  describe('EditView', () => {
    /* eslint-disable camelcase */
    const github = new SCMs.SCM({
      id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
      name:            'Github PR',
      auto_update:     true,
      plugin_metadata: {id: 'github.pr', version: '1.1'},
      configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
    });
    /* eslint-enable camelcase */

    const pluggableMaterial = Materials.create({
      type:        'plugin',
      scm:         github,
      filter:      new Materials.Filter({ignore: ['*.doc']}),
      destination: 'dest_folder'
    });

    beforeEach(() => {
      spyOn(PluginInfos.PluginInfo, 'get').and.returnValue($.Deferred().promise());
      spyOn(angular, 'bootstrap');
      mount(pluggableMaterial);
    });

    afterEach(() => {
      unmount();
    });

    it('should bind name', () => {
      expect($root.find(".pluggable-scm li:nth-child(1)>label")).toHaveText('Name');
      expect($root.find(".pluggable-scm li:nth-child(1)>span")).toHaveText(github.name());
    });

    it('should bind auto update field', () => {
      expect($root.find(".pluggable-scm li:nth-child(2)>label")).toHaveText('AutoUpdate');
      expect(Boolean($root.find(".pluggable-scm li:nth-child(2)>span").text())).toBe(github.autoUpdate());
    });

    it('should bind url configuration', () => {
      expect($root.find(".pluggable-scm li:nth-child(3)>label")).toHaveText('Url');
      expect($root.find(".pluggable-scm li:nth-child(3)>span")).toHaveText('path/to/repo');
    });

    it('should bind username configuration', () => {
      expect($root.find(".pluggable-scm li:nth-child(4)>label")).toHaveText('Username');
      expect($root.find(".pluggable-scm li:nth-child(4)>span")).toHaveText('some_name');
    });

    it('should bind the ignore fields', () => {
      expect($root.find("input[data-prop-name='ignore']")).toHaveValue('*.doc');
    });

    it('should bind destination', () => {
      expect($root.find("input[data-prop-name='destination']")).toHaveValue(pluggableMaterial.destination());
    });
  });

  describe('NewView', () => {
    beforeEach(() => {
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

    afterEach(() => {
      SCMs([]);
    });

    it('should have a dropdown to select a scm', () => {
      mount(Materials.create({
        type:       'plugin',
        pluginInfo: new PluginInfos.PluginInfo({id: 'github.pr', version: '1.0.0'})
      }));

      expect($('.scm-selector select option')).toHaveLength(1);
      expect($('.scm-selector select option')).toHaveText('material_1');
      expect($('.scm-selector select option')).toHaveValue('plugin_id_1');

      unmount();
    });

    it('should not have a dropdown in absence of matching scms', () => {
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
      view() {
        return m(PluggableSCMWidget, {material: pluggableMaterial});
      }
    });
    m.redraw();
  }

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

});
