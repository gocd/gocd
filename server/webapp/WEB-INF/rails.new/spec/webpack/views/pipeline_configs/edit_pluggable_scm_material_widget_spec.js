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

describe("Edit Pluggable SCM Material Widget", () => {
  const $      = require("jquery");
  const m      = require("mithril");
  const Stream = require("mithril/stream");

  const Materials                      = require("models/pipeline_configs/materials");
  const SCMs                           = require("models/pipeline_configs/scms");
  const EditPluggableSCMMaterialWidget = require("views/pipeline_configs/edit_pluggable_scm_material_widget");
  const PluginInfos                    = require("models/shared/plugin_infos");

  let $root, root, pluggableMaterial;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);


  describe('Show', () => {
    beforeEach(() => {
      SCMs([github]);
      pluggableMaterial = Materials.create({
        type:         "plugin",
        scm:          github,
        filter:       new Materials.Filter({ignore: ['*.doc']}),
        destination:  "dest_folder",
        invertFilter: true
      });
      spyOn(SCMs, 'findById').and.returnValue($.Deferred().resolve(github));
      mount(githubSCMJSON.id, pluggableMaterial);
    });

    afterEach(() => {
      SCMs([]);
      SCMs.scmIdToEtag = {};
      unmount();
    });

    it('should show the pluggable scm metadata', () => {
      expect($('label.name')).toContainText('Name');
      expect($root.find('span')[0]).toContainText('GitHub PR');

      expect($('label.autoupdate')).toContainText('AutoUpdate');
      expect($root.find('span')[1]).toContainText('true');

      expect($('label.url')).toContainText('Url');
      expect($root.find('span')[2]).toContainText('path/to/repo');

      expect($('label.username')).toContainText('Username');
      expect($root.find('span')[3]).toContainText('some_name');
    });

    it('should show the destination directory', () => {
      expect($root.find('input[data-prop-name="destination"]')[0].value).toBe('dest_folder');
    });

    it('should show ignore pattern', () => {
      expect($root.find('input[data-prop-name="ignore"]')[0].value).toBe('*.doc');
    });

    it('should show invert filter', () => {
      expect($root.find('input[data-prop-name="invertFilter"]')[0]).toBeChecked();
    });
  });

  function mount(scmId, material) {
    m.mount(root, {
      view() {
        return m(EditPluggableSCMMaterialWidget, {
          scmId:       Stream(scmId),
          material,
          pluginInfos: Stream(PluginInfos.fromJSON(pluginInfosJSON))
        });
      }
    });
    m.redraw();
  }

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  const pluginInfosJSON = [
    {
      "id":           "github.pr",
      "version":      "1",
      "type":         "scm",
      "status": {
        "state": "active"
      },
      "about":        {
        "name":                     "GitHub Pull Requests Builder",
        "version":                  "1.3.0-RC2",
        "target_go_version":        "15.1.0",
        "description":              "Plugin that polls a GitHub repository for pull requests and triggers a build for each of them",
        "target_operating_systems": [],
        "vendor":                   {
          "name": "Ashwanth Kumar",
          "url":  "https://github.com/ashwanthkumar/gocd-build-github-pull-requests"
        }
      },
      "scm_settings": {
        "configurations": [],
        "view":           {
          "template": "<div/>\n"
        }
      }
    }
  ];

  /* eslint-disable camelcase */
  const githubSCMJSON = {
    id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
    name:            'GitHub PR',
    auto_update:     true,
    plugin_metadata: {id: 'github.pr', version: '1.1'},
    configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
  };
  const github        = new SCMs.SCM(githubSCMJSON);
  /* eslint-enable camelcase */

});
