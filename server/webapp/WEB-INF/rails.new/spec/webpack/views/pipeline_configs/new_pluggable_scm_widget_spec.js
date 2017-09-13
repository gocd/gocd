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

describe("New Pluggable SCM Material Widget", () => {
  const $      = require("jquery");
  const m      = require("mithril");
  const Stream = require("mithril/stream");

  const Materials                     = require("models/pipeline_configs/materials");
  const SCMs                          = require("models/pipeline_configs/scms");
  const NewPluggableSCMMaterialWidget = require("views/pipeline_configs/new_pluggable_scm_material_widget");
  const PluginInfos                   = require("models/shared/plugin_infos");

  let $root, root, pluggableMaterial;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);


  describe('SCM Dropdown', () => {
    beforeEach(() => {
      SCMs([github]);
      pluggableMaterial = Materials.create({
        type:         "plugin",
        scm:          github,
        pluginInfo:   PluginInfos.fromJSON(pluginInfosJSON).firstPluginInfo(),
        filter:       new Materials.Filter({ignore: ['*.doc']}),
        destination:  "dest_folder",
        invertFilter: true
      });
      spyOn(SCMs, 'findById').and.returnValue($.Deferred().resolve(github));
      mount(pluggableMaterial);
    });

    afterEach(() => {
      SCMs([]);
      SCMs.scmIdToEtag = {};
      unmount();
    });

    it('should show the available pluggable scm list dropdown', () => {
      expect($root.find('select').children().size()).toBe(1);
      expect($root.find('select').children()[0]).toContainText(githubSCMJSON.name);
    });
  });

  describe('Empty SCM Dropdown', () => {
    beforeEach(() => {
      pluggableMaterial = Materials.create({
        type:         "plugin",
        scm:          github,
        pluginInfo:   PluginInfos.fromJSON(pluginInfosJSON).firstPluginInfo(),
        filter:       new Materials.Filter({ignore: ['*.doc']}),
        destination:  "dest_folder",
        invertFilter: true
      });
      spyOn(SCMs, 'findById').and.returnValue($.Deferred().resolve(null));
      mount(pluggableMaterial);
    });

    afterEach(() => {
      SCMs([]);
      SCMs.scmIdToEtag = {};
      unmount();
    });

    it('should show the available pluggable scm list dropdown', () => {
      expect($root.find('label')).toContainText('No existing SCMs for GitHub Pull Requests Builder');
    });
  });


  function mount(material) {
    m.mount(root, {
      view() {
        return m(NewPluggableSCMMaterialWidget, {
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
