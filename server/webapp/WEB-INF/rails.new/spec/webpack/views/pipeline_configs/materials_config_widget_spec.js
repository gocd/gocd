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
describe("Materials Config Widget", () => {
  const m      = require('mithril');
  const Stream = require('mithril/stream');


  require('jasmine-jquery');

  const Materials             = require("models/pipeline_configs/materials");
  const MaterialsConfigWidget = require("views/pipeline_configs/materials_config_widget");
  const PluginInfos           = require("models/shared/plugin_infos");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  describe('SVN View', () => {
    let material;
    beforeEach(() => {
      const materials = new Materials();
      material        = materials.createMaterial({
        type:           'svn',
        url:            "http://svn.example.com/svn/myProject",
        username:       "bob",
        password:       "p@ssw0rd",
        checkExternals: true,
        destination:    "projectA",
        name:           "svn-repo",
        autoUpdate:     true,
        filter:         new Materials.Filter({ignore: ['*.doc']}),
        invertFilter:   true
      });

      mount(materials);
      viewMaterial();
    });

    afterEach(() => {
      unmount();
    });

    it('should bind url', () => {
      expect($root.find("input[data-prop-name='url']")).toHaveValue(material.url());
    });

    it('should bind password', () => {
      expect($root.find("input[data-prop-name='passwordValue']")).toHaveValue("p@ssw0rd");
    });

    it('should bind username', () => {
      expect($root.find("input[data-prop-name='username']")).toHaveValue(material.username());
    });

    it('should bind checkExternals', () => {
      expect($root.find("input[data-prop-name='checkExternals']")).toHaveValue('on');
    });

    it('should bind name', () => {
      expect($root.find("input[data-prop-name='name']")).toHaveValue(material.name());
    });

    it('should bind destination', () => {
      expect($root.find("input[data-prop-name='destination']")).toHaveValue(material.destination());
    });

    it('should bind the ignore fields', () => {
      expect($root.find("input[data-prop-name='ignore']")).toHaveValue('*.doc');
    });

    it('should bind invertFilter', () => {
      expect($root.find("input[data-prop-name='invertFilter']")).toBeChecked();
    });

    it('should show tooltip message based on invertFilter', () => {
      expect($root.html()).toContain('(Optional) Enter the paths to be included while triggering pipelines. Separate multiple entries with a comma.');
    });

    it('should bind autoUpdate value', () => {
      expect($root.find("input[data-prop-name='autoUpdate']")).toBeChecked();
    });

    it('should show have material name in the header', () => {
      expect($root.find(".material-definitions li")).toContainText("svn-repo");
    });
  });

  describe('Git View', () => {
    let material;
    beforeEach(() => {
      const materials = new Materials();
      material        = materials.createMaterial({
        type:         'git',
        url:          "http://git.example.com/git/myProject",
        branch:       "release-1.2",
        destination:  "projectA",
        name:         "git-repo",
        autoUpdate:   false,
        filter:       new Materials.Filter({ignore: ['*.doc']}),
        shallowClone: true
      });

      mount(materials);
      viewMaterial();
    });

    afterEach(() => {
      unmount();
    });

    it('should bind url', () => {
      expect($root.find("input[data-prop-name='url']")).toHaveValue(material.url());
    });

    it('should bind branch', () => {
      expect($root.find("input[data-prop-name='branch']")).toHaveValue(material.branch());
    });

    it('should bind name', () => {
      expect($root.find("input[data-prop-name='name']")).toHaveValue(material.name());
    });

    it('should bind destination', () => {
      expect($root.find("input[data-prop-name='destination']")).toHaveValue(material.destination());
    });

    it('should bind the ignore fields', () => {
      expect($root.find("input[data-prop-name='ignore']")).toHaveValue('*.doc');
    });

    it('should bind invertFilter', () => {
      expect($root.find("input[data-prop-name='invertFilter']")).not.toBeChecked();
    });

    it('should bind autoUpdate value', () => {
      expect($root.find("input[data-prop-name='autoUpdate']")).not.toBeChecked();
    });

    it('should show tooltip message based on invertFilter', () => {
      expect($root.html()).toContain('(Optional) Enter the paths to be excluded while triggering pipelines. Separate multiple entries with a comma.');
    });

    it('should bind shallow clone value', () => {
      expect($root.find("input[data-prop-name='shallowClone']")).toBeChecked();
    });

    it('should show have material name in the header', () => {
      expect($root.find(".material-definitions li")).toContainText("git-repo");
    });
  });

  describe('Mercurial View', () => {
    let material;
    beforeEach(() => {
      const materials = new Materials();
      material        = materials.createMaterial({
        type:        'hg',
        url:         "http://hg.example.com/hg/myProject",
        destination: "projectA",
        name:        "hg-repo",
        autoUpdate:  true,
        filter:      new Materials.Filter({ignore: ['*.doc']})
      });

      mount(materials);
      viewMaterial();
    });

    afterEach(() => {
      unmount();
    });

    it('should bind url', () => {
      expect($root.find("input[data-prop-name='url']")).toHaveValue(material.url());
    });

    it('should bind name', () => {
      expect($root.find("input[data-prop-name='name']")).toHaveValue(material.name());
    });

    it('should bind destination', () => {
      expect($root.find("input[data-prop-name='destination']")).toHaveValue(material.destination());
    });

    it('should bind the ignore fields', () => {
      expect($root.find("input[data-prop-name='ignore']")).toHaveValue('*.doc');
    });

    it('should bind invertFilter', () => {
      expect($root.find("input[data-prop-name='invertFilter']")).not.toBeChecked();
    });

    it('should show tooltip message based on invertFilter', () => {
      expect($root.html()).toContain('(Optional) Enter the paths to be excluded while triggering pipelines. Separate multiple entries with a comma.');
    });

    it('should bind autoUpdate value', () => {
      expect($root.find("input[data-prop-name='autoUpdate']")).toBeChecked();
    });

    it('should show have material name in the header', () => {
      expect($root.find(".material-definitions li")).toContainText("hg-repo");
    });
  });

  describe('Perforce View', () => {
    let material;
    beforeEach(() => {
      const materials = new Materials();
      material        = materials.createMaterial({
        type:         'p4',
        port:         "p4.example.com:1666",
        username:     "bob",
        password:     "p@ssw0rd",
        useTickets:   true,
        destination:  "projectA",
        view:         "//depot/dev/source...          //anything/source/",
        name:         "perforce-repo",
        autoUpdate:   true,
        filter:       new Materials.Filter({ignore: ['*.doc']}),
        invertFilter: true
      });

      mount(materials);
      viewMaterial();
    });

    afterEach(() => {
      unmount();
    });

    it('should bind port', () => {
      expect($root.find("input[data-prop-name='port']")).toHaveValue(material.port());
    });

    it('should bind username', () => {
      expect($root.find("input[data-prop-name='username']")).toHaveValue(material.username());
    });

    it('should bind view', () => {
      expect($root.find("textarea[data-prop-name='view']")).toHaveValue(material.view());
    });

    it('should bind password value', () => {
      expect($root.find("input[data-prop-name='passwordValue']")).toHaveValue('p@ssw0rd');
    });

    it('should bind useTickets value', () => {
      expect($root.find("input[data-prop-name='useTickets']")).toHaveValue('on');
    });

    it('should bind name', () => {
      expect($root.find("input[data-prop-name='name']")).toHaveValue(material.name());
    });

    it('should bind destination', () => {
      expect($root.find("input[data-prop-name='destination']")).toHaveValue(material.destination());
    });

    it('should bind the ignore fields', () => {
      expect($root.find("input[data-prop-name='ignore']")).toHaveValue('*.doc');
    });

    it('should bind invertFilter', () => {
      expect($root.find("input[data-prop-name='invertFilter']")).toBeChecked();
    });

    it('should show tooltip message based on invertFilter', () => {
      expect($root.html()).toContain('(Optional) Enter the paths to be included while triggering pipelines. Separate multiple entries with a comma.');
    });

    it('should bind autoUpdate value', () => {
      expect($root.find("input[data-prop-name='autoUpdate']")).toBeChecked();
    });

    it('should show have material name in the header', () => {
      expect($root.find(".material-definitions li")).toContainText("perforce-repo");
    });
  });

  describe('TFS View', () => {
    let material;
    beforeEach(() => {
      const materials = new Materials();
      material        = materials.createMaterial({
        type:         'tfs',
        url:          "http://tfs.example.com/tfs/projectA",
        username:     "bob",
        password:     "p@ssw0rd",
        domain:       'AcmeCorp',
        destination:  "projectA",
        projectPath:  "$/webApp",
        name:         "tfs-repo",
        autoUpdate:   true,
        filter:       new Materials.Filter({ignore: ['*.doc']}),
        invertFilter: true
      });

      mount(materials);
      viewMaterial();
    });

    afterEach(() => {
      unmount();
    });

    it('should bind url', () => {
      expect($root.find("input[data-prop-name='url']")).toHaveValue(material.url());
    });

    it('should bind username', () => {
      expect($root.find("input[data-prop-name='username']")).toHaveValue(material.username());
    });

    it('should bind domain', () => {
      expect($root.find("input[data-prop-name='domain']")).toHaveValue(material.domain());
    });

    it('should bind password value', () => {
      expect($root.find("input[data-prop-name='passwordValue']")).toHaveValue('p@ssw0rd');
    });

    it('should bind projectPath', () => {
      expect($root.find("input[data-prop-name='projectPath']")).toHaveValue(material.projectPath());
    });

    it('should bind name', () => {
      expect($root.find("input[data-prop-name='name']")).toHaveValue(material.name());
    });

    it('should bind destination', () => {
      expect($root.find("input[data-prop-name='destination']")).toHaveValue(material.destination());
    });

    it('should bind the ignore fields', () => {
      expect($root.find("input[data-prop-name='ignore']")).toHaveValue('*.doc');
    });

    it('should bind invertFilter', () => {
      expect($root.find("input[data-prop-name='invertFilter']")).toBeChecked();
    });

    it('should show tooltip message based on invertFilter', () => {
      expect($root.html()).toContain('(Optional) Enter the paths to be included while triggering pipelines. Separate multiple entries with a comma.');
    });

    it('should bind autoUpdate value', () => {
      expect($root.find("input[data-prop-name='autoUpdate']")).toBeChecked();
    });

    it('should show have material name in the header', () => {
      expect($root.find(".material-definitions li")).toContainText("tfs-repo");
    });
  });

  describe('Dependency View', () => {
    let material;
    beforeEach(() => {
      const materials = new Materials();
      material        = materials.createMaterial({
        type:       'dependency',
        name:       'dependencyMaterial',
        pipeline:   'pipeline1',
        stage:      'stage1',
        autoUpdate: true
      });

      mount(materials);
      viewMaterial();
    });

    afterEach(() => {
      unmount();
    });

    it('should bind name', () => {
      expect($root.find("input[data-prop-name='name']")).toHaveValue(material.name());
    });

    it('should bind pipeline and stage', () => {
      expect($root.find("input[name='pipeline-stage']")).toHaveValue('pipeline1 [stage1]');
    });

    it('should show have material name in the header', () => {
      expect($root.find(".material-definitions li")).toContainText("dependencyMaterial");
    });
  });

  const mount = (materials) => {
    m.mount(root, {
      view() {
        return m(MaterialsConfigWidget, {
          materials:    Stream(materials),
          pluginInfos:  Stream(PluginInfos.fromJSON(pluginInfosJSON)),
          pipelineName: Stream('testPipeLine'),
          pipelines:    Stream([])
        });
      }
    });
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  const pluginInfosJSON = [
    {
      "id":      "github.oauth.login",
      "version": "1",
      "type":    "authentication",
      "status": {
        "state": "active"
      },
      "about":   {
        "name":                     "GitHub OAuth Login",
        "version":                  "2.2",
        "target_go_version":        "16.2.1",
        "description":              "Login using GitHub OAuth",
        "target_operating_systems": ['Linux', 'Mac'],
        "vendor":                   {
          "name": "GoCD Contributors",
          "url":  "https://github.com/gocd-contrib/gocd-oauth-login"
        }
      }
    }
  ];

  function viewMaterial() {
    $root.find(".materials>.accordion-item>a")[0].click();
    m.redraw();
    $root.find('.material-definitions>.accordion-item>a')[0].click();
    m.redraw();
  }
});
