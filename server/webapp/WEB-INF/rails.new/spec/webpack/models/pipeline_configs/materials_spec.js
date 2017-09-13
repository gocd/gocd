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

describe("Material Model", () => {

  const Stream      = require('mithril/stream');
  const Materials   = require("models/pipeline_configs/materials");
  const SCMs        = require("models/pipeline_configs/scms");
  const PluginInfos = require('models/shared/plugin_infos');

  let materials, gitMaterial, svnMaterial, mercurialMaterial, perforceMaterial, tfsMaterial, dependencyMaterial;
  afterEach(() => {
    SCMs([]);
    SCMs.scmIdToEtag = {};
  });

  beforeEach(() => {
    materials = new Materials();

    gitMaterial = materials.createMaterial({
      type:         'git',
      url:          "http://git.example.com/git/myProject",
      branch:       "release-1.2",
      destination:  "projectA",
      name:         "git-repo",
      autoUpdate:   true,
      filter:       new Materials.Filter({ignore: ['*.doc']}),
      invertFilter: true,
      shallowClone: true
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
      filter:         new Materials.Filter({ignore: ['*.doc']}),
      invertFilter:   true
    });

    mercurialMaterial = materials.createMaterial({
      type:         'hg',
      url:          "http://hg.example.com/hg/myProject",
      branch:       "release-1.2",
      destination:  "projectA",
      name:         "hg-repo",
      autoUpdate:   true,
      filter:       new Materials.Filter({ignore: ['*.doc']}),
      invertFilter: true
    });

    perforceMaterial = materials.createMaterial({
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

    tfsMaterial = materials.createMaterial({
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

    dependencyMaterial = materials.createMaterial({
      type:       "dependency",
      pipeline:   "p1",
      stage:      "first_stage",
      name:       "p1_first_stage",
      autoUpdate: true
    });
  });

  describe("validation", () => {
    it("should not allow materials with duplicate names", () => {
      let errorsOnOriginal = gitMaterial.validate();
      expect(errorsOnOriginal._isEmpty()).toBe(true);

      const duplicate = materials.createMaterial({
        type: 'git',
        name: gitMaterial.name()
      });

      errorsOnOriginal = gitMaterial.validate();
      expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

      const errorsOnDuplicate = duplicate.validate();
      expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
    });

    it("should allow multiple materials to have blank names", () => {
      const materialA = materials.createMaterial({
        type: 'git',
        name: '',
        url:  'https://github.com/gocd/gocd'
      });

      const materialB = materials.createMaterial({
        type: 'git',
        name: '',
        url:  'https://github.com/gocd/website'
      });

      const errorsOnA = materialA.validate();
      expect(errorsOnA.hasErrors('name')).toBe(false);

      const errorsOnB = materialB.validate();
      expect(errorsOnB.hasErrors('name')).toBe(false);
    });
  });

  describe('Test Connection', () => {
    describe('success', () => {
      it('should post to material_test url', () => {
        const material = new Materials().createMaterial({
          type: 'git',
          url:  "http://git.example.com/git/myProject"
        });

        jasmine.Ajax.withMock(() => {
          jasmine.Ajax.stubRequest('/go/api/admin/internal/material_test', undefined, 'POST').andReturn({
            responseText: JSON.stringify({status: 'success'}),
            status:       200,
            headers:      {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            }
          });

          const successCallback = jasmine.createSpy();

          material.testConnection(Stream('testPipeline')).then(successCallback);
          expect(successCallback).toHaveBeenCalled();
        });
      });
    });

    describe('failure', () => {
      it('should post to material_test url', () => {
        const material = new Materials().createMaterial({
          type: 'git',
          url:  "http://git.example.com/git/myProject"
        });

        jasmine.Ajax.withMock(() => {
          jasmine.Ajax.stubRequest('/go/api/admin/internal/material_test', undefined, 'POST').andReturn({
            responseText: JSON.stringify({status: 'failure'}),
            status:       500,
            headers:      {
              'Content-Type': 'application/vnd.go.cd.v1+json'
            }
          });

          const errorMessage = jasmine.createSpy().and.callFake((errorMessage) => {
            expect(errorMessage).toBe('There was an unknown error while checking connection');
          });

          material.testConnection(Stream('testPipeline')).fail(errorMessage);
          expect(errorMessage).toHaveBeenCalled();
        });
      });
    });
  });

  describe("Material Type", () => {
    describe("SVN", () => {
      it("should initialize material model with type", () => {
        expect(svnMaterial.type()).toBe("svn");
      });

      it("should initialize material model with url", () => {
        expect(svnMaterial.url()).toBe("http://svn.example.com/svn/myProject");
      });

      it("should initialize material model with username", () => {
        expect(svnMaterial.username()).toBe("bob");
      });

      it("should initialize material model with password", () => {
        expect(svnMaterial.passwordValue()).toBe("p@ssw0rd");
      });

      it("should initialize material model with checkExternals", () => {
        expect(svnMaterial.checkExternals()).toBe(true);
      });

      it("should initialize material model with destination", () => {
        expect(svnMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with name", () => {
        expect(svnMaterial.name()).toBe("svn-repo");
      });

      it("should initialize material model with autoUpdate", () => {
        expect(svnMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", () => {
        expect(svnMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with invert_filter", () => {
        expect(svnMaterial.invertFilter()).toBe(true);
      });

      describe("validation", () => {
        it("should add error when url is blank", () => {
          svnMaterial.url("");
          const errors = svnMaterial.validate();
          expect(errors.errors('url')).toEqual(['URL must be present']);
        });
      });

      describe("Deserialization from JSON", () => {
        beforeEach(() => {
          svnMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should initialize from json", () => {
          expect(svnMaterial.type()).toBe("svn");
          expect(svnMaterial.url()).toBe("http://svn.example.com/svn/myProject");
          expect(svnMaterial.username()).toBe("bob");
          expect(svnMaterial.passwordValue()).toBe("p@ssw0rd");
          expect(svnMaterial.checkExternals()).toBe(true);
          expect(svnMaterial.destination()).toBe("projectA");
          expect(svnMaterial.name()).toBe("materialA");
          expect(svnMaterial.autoUpdate()).toBe(true);
          expect(svnMaterial.filter().ignore()).toEqual(['*.doc']);
          expect(svnMaterial.invertFilter()).toBe(true);
        });

        it('should map server side errors', () => {
          const material = Materials.Material.fromJSON({
            type:   "svn",
            errors: {
              url: [
                "URL cannot be empty"
              ]
            }
          });

          expect(material.errors()._isEmpty()).toBe(false);
          expect(material.errors().errors('url')).toEqual(['URL cannot be empty']);
        });

        function sampleJSON() {
          /* eslint-disable camelcase */
          return {
            type:       "svn",
            attributes: {
              url:             "http://svn.example.com/svn/myProject",
              username:        "bob",
              password:        "p@ssw0rd",
              check_externals: true,
              destination:     "projectA",
              name:            "materialA",
              auto_update:     true,
              filter:          {
                ignore: ['*.doc']
              },
              invert_filter:   true
            }
          };
          /* eslint-enable camelcase */
        }
      });
    });

    describe("Git", () => {
      it("should initialize material model with type", () => {
        expect(gitMaterial.type()).toBe("git");
      });

      it("should initialize material model with url", () => {
        expect(gitMaterial.url()).toBe("http://git.example.com/git/myProject");
      });

      it("should initialize material model with branch", () => {
        expect(gitMaterial.branch()).toBe("release-1.2");
      });

      it("should initialize material model with destination", () => {
        expect(gitMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with name", () => {
        expect(gitMaterial.name()).toBe("git-repo");
      });

      it("should initialize material model with autoUpdate", () => {
        expect(gitMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", () => {
        expect(gitMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with shallow clone", () => {
        expect(gitMaterial.shallowClone()).toBe(true);
      });

      it("should initialize material model with invert_filter", () => {
        expect(gitMaterial.invertFilter()).toBe(true);
      });

      describe("validation", () => {
        it("should add error when url is blank", () => {
          gitMaterial.url("");
          const errors = gitMaterial.validate();
          expect(errors.errors('url')).toEqual(['URL must be present']);
        });
      });

      describe("Default Value", () => {
        beforeEach(() => {
          gitMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should use the default branch value when not provided", () => {
          expect(gitMaterial.branch()).toBe('master');
        });

        function sampleJSON() {
          return {
            type:       "git",
            attributes: {
              url:    "http://git.example.com/git/myProject",
              branch: null,
            }
          };
        }
      });

      describe("Deserialization from JSON", () => {
        beforeEach(() => {
          gitMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should initialize from json", () => {
          expect(gitMaterial.type()).toBe("git");
          expect(gitMaterial.url()).toBe("http://git.example.com/git/myProject");
          expect(gitMaterial.branch()).toBe('release-1.2');
          expect(gitMaterial.destination()).toBe("projectA");
          expect(gitMaterial.name()).toBe("materialA");
          expect(gitMaterial.autoUpdate()).toBe(true);
          expect(gitMaterial.filter().ignore()).toEqual(['*.doc']);
          expect(gitMaterial.shallowClone()).toBe(true);
          expect(gitMaterial.invertFilter()).toBe(true);
        });

        it('should map server side errors', () => {
          const material = Materials.Material.fromJSON({
            type:   "git",
            errors: {
              url: [
                "URL cannot be empty"
              ]
            }
          });

          expect(material.errors()._isEmpty()).toBe(false);
          expect(material.errors().errors('url')).toEqual(['URL cannot be empty']);
        });

        function sampleJSON() {
          /* eslint-disable camelcase */
          return {
            type:       "git",
            attributes: {
              url:           "http://git.example.com/git/myProject",
              branch:        "release-1.2",
              destination:   "projectA",
              name:          "materialA",
              auto_update:   true,
              filter:        {
                ignore: ['*.doc']
              },
              shallow_clone: true,
              invert_filter: true
            }
          };
          /* eslint-enable camelcase */
        }
      });
    });

    describe("Mercurial", () => {
      it("should initialize material model with type", () => {
        expect(mercurialMaterial.type()).toBe("hg");
      });

      it("should initialize material model with url", () => {
        expect(mercurialMaterial.url()).toBe("http://hg.example.com/hg/myProject");
      });

      it("should initialize material model with branch", () => {
        expect(mercurialMaterial.branch()).toBe("release-1.2");
      });

      it("should initialize material model with destination", () => {
        expect(mercurialMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with name", () => {
        expect(mercurialMaterial.name()).toBe("hg-repo");
      });

      it("should initialize material model with autoUpdate", () => {
        expect(mercurialMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", () => {
        expect(mercurialMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with invert_filter", () => {
        expect(mercurialMaterial.invertFilter()).toBe(true);
      });

      describe("validation", () => {
        it("should add error when url is blank", () => {
          mercurialMaterial.url("");
          const errors = mercurialMaterial.validate();
          expect(errors.errors('url')).toEqual(['URL must be present']);
        });
      });

      describe("Deserialization from JSON", () => {
        beforeEach(() => {
          mercurialMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should initialize from json", () => {
          expect(mercurialMaterial.type()).toBe("hg");
          expect(mercurialMaterial.url()).toBe("http://hg.example.com/hg/myProject");
          expect(mercurialMaterial.branch()).toBe('release-1.2');
          expect(mercurialMaterial.destination()).toBe("projectA");
          expect(mercurialMaterial.name()).toBe("materialA");
          expect(mercurialMaterial.autoUpdate()).toBe(true);
          expect(mercurialMaterial.filter().ignore()).toEqual(['*.doc']);
          expect(mercurialMaterial.invertFilter()).toBe(true);
        });

        it('should map server side errors', () => {
          const material = Materials.Material.fromJSON({
            type:   "hg",
            errors: {
              url: [
                "URL cannot be empty"
              ]
            }
          });

          expect(material.errors()._isEmpty()).toBe(false);
          expect(material.errors().errors('url')).toEqual(['URL cannot be empty']);
        });

        function sampleJSON() {
          /* eslint-disable camelcase */
          return {
            type:       "hg",
            attributes: {
              url:           "http://hg.example.com/hg/myProject",
              branch:        "release-1.2",
              destination:   "projectA",
              name:          "materialA",
              auto_update:   true,
              filter:        {
                ignore: ['*.doc']
              },
              invert_filter: true
            }
          };
          /* eslint-enable camelcase */
        }
      });
    });

    describe("Perforce", () => {
      it("should initialize material model with type", () => {
        expect(perforceMaterial.type()).toBe("p4");
      });

      it("should initialize material model with port", () => {
        expect(perforceMaterial.port()).toBe("p4.example.com:1666");
      });

      it("should initialize material model with username", () => {
        expect(perforceMaterial.username()).toBe("bob");
      });

      it("should initialize material model with password", () => {
        expect(perforceMaterial.passwordValue()).toBe("p@ssw0rd");
      });

      it("should initialize material model with useTickets", () => {
        expect(perforceMaterial.useTickets()).toBe(true);
      });

      it("should initialize material model with destination", () => {
        expect(perforceMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with view", () => {
        expect(perforceMaterial.view()).toBe("//depot/dev/source...          //anything/source/");
      });

      it("should initialize material model with name", () => {
        expect(perforceMaterial.name()).toBe("perforce-repo");
      });

      it("should initialize material model with autoUpdate", () => {
        expect(perforceMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", () => {
        expect(perforceMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with invert_filter", () => {
        expect(perforceMaterial.invertFilter()).toBe(true);
      });

      describe("validation", () => {
        it("should add error when port is blank", () => {
          perforceMaterial.port("");
          const errors = perforceMaterial.validate();
          expect(errors.errors('port')).toEqual(['Port must be present']);
        });

        it("should add error when view is blank", () => {
          perforceMaterial.view("");
          const errors = perforceMaterial.validate();
          expect(errors.errors('view')).toEqual(['View must be present']);
        });
      });

      describe("Deserialization from JSON", () => {
        beforeEach(() => {
          perforceMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should initialize from json", () => {
          expect(perforceMaterial.type()).toBe("p4");
          expect(perforceMaterial.port()).toBe("p4.example.com:1666");
          expect(perforceMaterial.username()).toBe("bob");
          expect(perforceMaterial.passwordValue()).toBe("p@ssw0rd");
          expect(perforceMaterial.useTickets()).toBe(true);
          expect(perforceMaterial.destination()).toBe("projectA");
          expect(perforceMaterial.name()).toBe("materialA");
          expect(perforceMaterial.autoUpdate()).toBe(true);
          expect(perforceMaterial.view()).toBe("//depot/dev/source...          //anything/source/");
          expect(perforceMaterial.filter().ignore()).toEqual(['*.doc']);
          expect(perforceMaterial.invertFilter()).toBe(true);
        });

        it('should map server side errors', () => {
          const material = Materials.Material.fromJSON({
            type:   "p4",
            errors: {
              view: [
                "View cannot be empty"
              ],
              port: [
                "Port cannot be empty"
              ]
            }
          });

          expect(material.errors()._isEmpty()).toBe(false);
          expect(material.errors().errors('view')).toEqual(['View cannot be empty']);
          expect(material.errors().errors('port')).toEqual(['Port cannot be empty']);
        });

        function sampleJSON() {
          /* eslint-disable camelcase */
          return {
            type:       "p4",
            attributes: {
              port:          "p4.example.com:1666",
              username:      "bob",
              password:      "p@ssw0rd",
              use_tickets:   true,
              destination:   "projectA",
              view:          "//depot/dev/source...          //anything/source/",
              name:          "materialA",
              auto_update:   true,
              filter:        {
                ignore: ['*.doc']
              },
              invert_filter: true
            }
          };
          /* eslint-enable camelcase */
        }
      });
    });

    describe("TFS", () => {
      it("should initialize material model with type", () => {
        expect(tfsMaterial.type()).toBe("tfs");
      });

      it("should initialize material model with url", () => {
        expect(tfsMaterial.url()).toBe("http://tfs.example.com/tfs/projectA");
      });

      it("should initialize material model with username", () => {
        expect(tfsMaterial.username()).toBe("bob");
      });

      it("should initialize material model with password", () => {
        expect(tfsMaterial.passwordValue()).toBe("p@ssw0rd");
      });

      it("should initialize material model with domain", () => {
        expect(tfsMaterial.domain()).toBe('AcmeCorp');
      });

      it("should initialize material model with destination", () => {
        expect(tfsMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with projectPath", () => {
        expect(tfsMaterial.projectPath()).toBe("$/webApp");
      });

      it("should initialize material model with name", () => {
        expect(tfsMaterial.name()).toBe("tfs-repo");
      });

      it("should initialize material model with autoUpdate", () => {
        expect(tfsMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", () => {
        expect(tfsMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with invert_filter", () => {
        expect(tfsMaterial.invertFilter()).toBe(true);
      });

      describe("validation", () => {
        it("should add error when url is blank", () => {
          tfsMaterial.url("");
          const errors = tfsMaterial.validate();
          expect(errors.errors('url')).toEqual(['URL must be present']);
        });

        it("should add error when username is blank", () => {
          tfsMaterial.username("");
          const errors = tfsMaterial.validate();
          expect(errors.errors('username')).toEqual(['Username must be present']);
        });

        it("should add error when projectPath is blank", () => {
          tfsMaterial.projectPath("");
          const errors = tfsMaterial.validate();
          expect(errors.errors('projectPath')).toEqual(['Project path must be present']);
        });
      });

      describe("Serialization/De-serialization to/from JSON", () => {
        beforeEach(() => {
          tfsMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should serialize to JSON", () => {
          expect(tfsMaterial.toJSON()).toEqual(sampleJSON());
        });

        it("should de-serialize from JSON", () => {
          expect(tfsMaterial.type()).toBe("tfs");
          expect(tfsMaterial.url()).toBe("http://tfs.example.com/tfs/projectA");
          expect(tfsMaterial.username()).toBe("bob");
          expect(tfsMaterial.passwordValue()).toBe("p@ssw0rd");
          expect(tfsMaterial.domain()).toBe('AcmeCorp');
          expect(tfsMaterial.destination()).toBe("projectA");
          expect(tfsMaterial.name()).toBe("materialA");
          expect(tfsMaterial.autoUpdate()).toBe(true);
          expect(tfsMaterial.projectPath()).toBe("$/webApp");
          expect(tfsMaterial.filter().ignore()).toEqual(['*.doc']);
          expect(tfsMaterial.invertFilter()).toBe(true);
        });

        it('should map server side errors', () => {
          const material = Materials.Material.fromJSON({
            type:   "tfs",
            errors: {
              url:         [
                "URL cannot be empty"
              ],
              username:    [
                "Username cannot be empty"
              ],
              projectPath: [
                "ProjectPath cannot be empty"
              ]
            }
          });

          expect(material.errors()._isEmpty()).toBe(false);
          expect(material.errors().errors('url')).toEqual(['URL cannot be empty']);
          expect(material.errors().errors('username')).toEqual(['Username cannot be empty']);
          expect(material.errors().errors('projectPath')).toEqual(['ProjectPath cannot be empty']);
        });

        function sampleJSON() {
          /* eslint-disable camelcase */
          return {
            type:       "tfs",
            attributes: {
              name:          "materialA",
              auto_update:   true,
              filter:        {
                ignore: ['*.doc']
              },
              url:           "http://tfs.example.com/tfs/projectA",
              username:      "bob",
              password:      "p@ssw0rd",
              domain:        'AcmeCorp',
              destination:   "projectA",
              project_path:  "$/webApp",
              invert_filter: true
            }
          };
          /* eslint-enable camelcase */
        }
      });
    });

    describe('dependency', () => {
      it('it should initialize material with type', () => {
        expect(dependencyMaterial.type()).toBe('dependency');
      });

      it('it should initialize material with pipeline', () => {
        expect(dependencyMaterial.pipeline()).toBe('p1');
      });

      it('it should initialize material with stage', () => {
        expect(dependencyMaterial.stage()).toBe('first_stage');
      });

      it('it should initialize material with name', () => {
        expect(dependencyMaterial.name()).toBe('p1_first_stage');
      });

      describe("validation", () => {
        let errors;
        beforeEach(() => {
          dependencyMaterial.pipeline('');
          dependencyMaterial.stage('');
          errors = dependencyMaterial.validate();
        });

        it("should check presence of pipeline", () => {
          expect(errors.errors('pipeline')).toEqual(['Pipeline must be present']);
        });

        it("should check presence of stage", () => {
          expect(errors.errors('stage')).toEqual(['Stage must be present']);
        });
      });

      describe("Serialization/De-serialization to/from JSON", () => {
        beforeEach(() => {
          dependencyMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should serialize to JSON", () => {
          expect(dependencyMaterial.toJSON()).toEqual(sampleJSON());
        });

        it("should de-serialize from JSON", () => {
          expect(dependencyMaterial.type()).toBe("dependency");
          expect(dependencyMaterial.name()).toBe("materialA");
          expect(dependencyMaterial.pipeline()).toBe('p1');
          expect(dependencyMaterial.stage()).toEqual('s1');
        });

        it('should map server side errors', () => {
          const material = Materials.Material.fromJSON({
            type:   "p4",
            errors: {
              pipeline: [
                "Pipeline cannot be empty"
              ],
              stage:    [
                "Stage cannot be empty"
              ]
            }
          });

          expect(material.errors()._isEmpty()).toBe(false);
          expect(material.errors().errors('pipeline')).toEqual(['Pipeline cannot be empty']);
          expect(material.errors().errors('stage')).toEqual(['Stage cannot be empty']);
        });

        function sampleJSON() {
          return {
            type:       'dependency',
            attributes: {
              name:     'materialA',
              pipeline: 'p1',
              stage:    's1'
            }
          };
        }
      });
    });

    describe('plugin', () => {
      let pluggableMaterial;
      const github = new SCMs.SCM({
        /* eslint-disable camelcase */
        id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
        name:            'GitHub PR',
        auto_update:     true,
        plugin_metadata: {id: 'github.pr', version: '1.1'},
        configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
        /* eslint-enable camelcase */
      });


      beforeEach(() => {
        SCMs([github]);
        pluggableMaterial = Materials.create({
          type:         "plugin",
          scm:          github,
          filter:       new Materials.Filter({ignore: ['*.doc']}),
          destination:  "dest_folder",
          invertFilter: true
        });
      });

      afterEach(() => {
        SCMs([]);
      });

      it('it should initialize material with type', () => {
        expect(pluggableMaterial.type()).toBe('plugin');
      });

      it('it should initialize material with scm', () => {
        expect(pluggableMaterial.scm().id()).toBe('43c45e0b-1b0c-46f3-a60a-2bbc5cec069c');
      });

      it("should initialize material model with filters", () => {
        expect(pluggableMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with destination", () => {
        expect(pluggableMaterial.destination()).toBe('dest_folder');
      });

      it("should initialize material model with pluginInfo", () => {
        const data = {
          type: 'scm',
          id:   'plugin_id',
          status: {
            state: "active"
          }
        };
        expect(Materials.create({pluginInfo: PluginInfos.PluginInfo.fromJSON(data)}).pluginInfo().id()).toBe('plugin_id');
      });

      it("should initialize material model with invert_filter", () => {
        expect(pluggableMaterial.invertFilter()).toBe(true);
      });

      describe("Serialization/De-serialization to/from JSON", () => {
        beforeEach(() => {
          pluggableMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should serialize to JSON", () => {
          expect(pluggableMaterial.toJSON()).toEqual(sampleJSON());
        });

        it("should de-serialize from JSON", () => {
          expect(pluggableMaterial.type()).toBe("plugin");
          expect(pluggableMaterial.scm().id()).toBe("43c45e0b-1b0c-46f3-a60a-2bbc5cec069c");
          expect(pluggableMaterial.destination()).toBe('dest_folder');
          expect(pluggableMaterial.filter().ignore()).toEqual(['*.doc']);
          expect(pluggableMaterial.invertFilter()).toBe(true);
        });

        function sampleJSON() {
          /* eslint-disable camelcase */
          return {
            type:       'plugin',
            attributes: {
              ref:           '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
              destination:   'dest_folder',
              filter:        {
                ignore: ['*.doc']
              },
              invert_filter: true
            }
          };
          /* eslint-enable camelcase */
        }
      });
    });
  });
});
