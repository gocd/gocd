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

define(["jquery", "mithril", "models/pipeline_configs/materials", "views/pipeline_configs/materials_config_widget"], function ($, m, Materials, MaterialsConfigWidget) {
  describe("Material Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);
    beforeAll(function() {
      spyOn(m, 'request').and.callFake(function() {
        return $.Deferred().promise();
      });
    });

    describe('SVN View', function () {
      var material;
      beforeAll(function () {
        var materials = new Materials();
        material = materials.createMaterial({
          type:           'svn',
          url:            "http://svn.example.com/svn/myProject",
          username:       "bob",
          password:       "p@ssw0rd",
          checkExternals: true,
          destination:    "projectA",
          name:           "svn-repo",
          autoUpdate:     true,
          filter:         new Materials.Filter({ignore: ['*.doc']})
        });

        mount(materials);
        viewMaterial();
      });

      it('should bind url', function () {
        expect($root.find("input[data-prop-name='url']").val()).toBe(material.url());
      });

      it('should bind password', function () {
        expect($root.find("input[data-prop-name='passwordValue']").val()).toBe("p@ssw0rd");
      });

      it('should bind username', function () {
        expect($root.find("input[data-prop-name='username']").val()).toBe(material.username());
      });

      it('should bind checkExternals', function () {
        expect($root.find("input[data-prop-name='checkExternals']").val()).toBe('on');
      });

      it('should bind name', function () {
        expect($root.find("input[data-prop-name='name']").val()).toBe(material.name());
      });

      it('should bind destination', function () {
        expect($root.find("input[data-prop-name='destination']").val()).toBe(material.destination());
      });

      it('should bind the ignore fields', function () {
        expect($root.find("input[data-prop-name='ignore']").val()).toBe('*.doc');
      });

      it('should bind autoUpdate value', function () {
        expect($root.find("input[data-prop-name='autoUpdate']").val()).toBe('on');
      });
    });

    describe('Git View', function () {
      var material;
      beforeAll(function () {
        var materials = new Materials();
        material = materials.createMaterial({
          type:         'git',
          url:          "http://git.example.com/git/myProject",
          branch:       "release-1.2",
          destination:  "projectA",
          name:         "git-repo",
          autoUpdate:   true,
          filter:       new Materials.Filter({ignore: ['*.doc']}),
          shallowClone: true
        });

        mount(materials);
        viewMaterial();
      });

      it('should bind url', function () {
        expect($root.find("input[data-prop-name='url']").val()).toBe(material.url());
      });

      it('should bind branch', function () {
        expect($root.find("input[data-prop-name='branch']").val()).toBe(material.branch());
      });

      it('should bind name', function () {
        expect($root.find("input[data-prop-name='name']").val()).toBe(material.name());
      });

      it('should bind destination', function () {
        expect($root.find("input[data-prop-name='destination']").val()).toBe(material.destination());
      });

      it('should bind the ignore fields', function () {
        expect($root.find("input[data-prop-name='ignore']").val()).toBe('*.doc');
      });

      it('should bind autoUpdate value', function () {
        expect($root.find("input[data-prop-name='autoUpdate']").val()).toBe('on');
      });

      it('should bind shallow clone value', function () {
        expect($root.find("input[data-prop-name='shallowClone']").val()).toBe('on');
      });
    });

    describe('Mercurial View', function () {
      var material;
      beforeAll(function () {
        var materials = new Materials();
        material = materials.createMaterial({
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

      it('should bind url', function () {
        expect($root.find("input[data-prop-name='url']").val()).toBe(material.url());
      });

      it('should bind name', function () {
        expect($root.find("input[data-prop-name='name']").val()).toBe(material.name());
      });

      it('should bind destination', function () {
        expect($root.find("input[data-prop-name='destination']").val()).toBe(material.destination());
      });

      it('should bind the ignore fields', function () {
        expect($root.find("input[data-prop-name='ignore']").val()).toBe('*.doc');
      });

      it('should bind autoUpdate value', function () {
        expect($root.find("input[data-prop-name='autoUpdate']").val()).toBe('on');
      });
    });

    describe('Perforce View', function () {
      var material;
      beforeAll(function () {
        var materials = new Materials();
        material = materials.createMaterial({
          type:        'p4',
          port:        "p4.example.com:1666",
          username:    "bob",
          password:    "p@ssw0rd",
          useTickets:  true,
          destination: "projectA",
          view:        "//depot/dev/source...          //anything/source/",
          name:        "perforce-repo",
          autoUpdate:  true,
          filter:      new Materials.Filter({ignore: ['*.doc']})
        });

        mount(materials);
        viewMaterial();
      });

      it('should bind port', function () {
        expect($root.find("input[data-prop-name='port']").val()).toBe(material.port());
      });

      it('should bind username', function () {
        expect($root.find("input[data-prop-name='username']").val()).toBe(material.username());
      });

      it('should bind view', function () {
        expect($root.find("textarea[data-prop-name='view']").val()).toBe(material.view());
      });

      it('should bind password value', function () {
        expect($root.find("input[data-prop-name='passwordValue']").val()).toBe('p@ssw0rd');
      });

      it('should bind useTickets value', function () {
        expect($root.find("input[data-prop-name='useTickets']").val()).toBe('on');
      });

      it('should bind name', function () {
        expect($root.find("input[data-prop-name='name']").val()).toBe(material.name());
      });

      it('should bind destination', function () {
        expect($root.find("input[data-prop-name='destination']").val()).toBe(material.destination());
      });

      it('should bind the ignore fields', function () {
        expect($root.find("input[data-prop-name='ignore']").val()).toBe('*.doc');
      });

      it('should bind autoUpdate value', function () {
        expect($root.find("input[data-prop-name='autoUpdate']").val()).toBe('on');
      });
    });

    describe('TFS View', function () {
      var material;
      beforeAll(function () {
        var materials = new Materials();
        material = materials.createMaterial({
          type:        'tfs',
          url:         "http://tfs.example.com/tfs/projectA",
          username:    "bob",
          password:    "p@ssw0rd",
          domain:      'AcmeCorp',
          destination: "projectA",
          projectPath: "$/webApp",
          name:        "tfs-repo",
          autoUpdate:  true,
          filter:      new Materials.Filter({ignore: ['*.doc']})
        });

        mount(materials);
        viewMaterial();
      });

      it('should bind url', function () {
        expect($root.find("input[data-prop-name='url']").val()).toBe(material.url());
      });

      it('should bind username', function () {
        expect($root.find("input[data-prop-name='username']").val()).toBe(material.username());
      });

      it('should bind domain', function () {
        expect($root.find("input[data-prop-name='domain']").val()).toBe(material.domain());
      });

      it('should bind password value', function () {
        expect($root.find("input[data-prop-name='passwordValue']").val()).toBe('p@ssw0rd');
      });

      it('should bind projectPath', function () {
        expect($root.find("input[data-prop-name='projectPath']").val()).toBe(material.projectPath());
      });

      it('should bind name', function () {
        expect($root.find("input[data-prop-name='name']").val()).toBe(material.name());
      });

      it('should bind destination', function () {
        expect($root.find("input[data-prop-name='destination']").val()).toBe(material.destination());
      });

      it('should bind the ignore fields', function () {
        expect($root.find("input[data-prop-name='ignore']").val()).toBe('*.doc');
      });

      it('should bind autoUpdate value', function () {
        expect($root.find("input[data-prop-name='autoUpdate']").val()).toBe('on');
      });
    });

    describe('Dependency View', function () {
      var material;
      beforeAll(function () {
        var materials = new Materials();
        material = materials.createMaterial({
          type:       'dependency',
          name:       'dependencyMaterial',
          pipeline:   'pipeline1',
          stage:      'stage1',
          autoUpdate: true
        });

        mount(materials);
        viewMaterial();
      });

      it('should bind name', function () {
        expect($root.find("input[data-prop-name='name']").val()).toBe(material.name());
      });

      it('should bind pipeline and stage', function () {
        expect($root.find("input[name='pipeline-stage']").val()).toBe('pipeline1 [stage1]');
      });
    });

    function mount(materials) {
      m.mount(root,
        m.component(MaterialsConfigWidget, {materials: m.prop(materials), pipelineName: m.prop('testPipeLine')})
      );
      m.redraw(true);
    }

    function viewMaterial() {
      $root.find(".materials>.accordion-item>a")[0].click();
      m.redraw(true);
      $root.find('.material-definitions>.accordion-item>a')[0].click();
      m.redraw(true);
    }
  });
});
