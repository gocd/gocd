/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(["jquery", "mithril", "pipeline_configs/models/materials", "pipeline_configs/views/materials_config_widget"], function ($, m, Materials, MaterialsConfigWidget) {
  describe("Material Widget", function () {
    var $root;
    var materials, gitMaterial, svnMaterial, mercurialMaterial, perforceMaterial, tfsMaterial;
    beforeEach(function () {
      materials = new Materials();

      gitMaterial = materials.createMaterial({
        type:        'git',
        url:         "http://git.example.com/git/myProject",
        branch:      "release-1.2",
        destination: "projectA",
        name:        "git-repo",
        autoUpdate:  true,
        filter:      new Materials.Filter({ignore: ['*.doc']})
      });

      svnMaterial = materials.createMaterial({
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

      mercurialMaterial = materials.createMaterial({
        type:        'hg',
        url:         "http://hg.example.com/hg/myProject",
        branch:      "release-1.2",
        destination: "projectA",
        name:        "hg-repo",
        autoUpdate:  true,
        filter:      new Materials.Filter({ignore: ['*.doc']})
      });

      perforceMaterial = materials.createMaterial({
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

      tfsMaterial = materials.createMaterial({
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

      var root = document.createElement("div");
      $root    = $(root);

      m.render(root,
        m.component(MaterialsConfigWidget, {materials: materials, selectedMaterialIndex: m.prop(0)})
      );
    });

    describe("SVN", function () {
      it("should foo", function () {
        expect(true).toBe(true);
      });

    });

  });
});
