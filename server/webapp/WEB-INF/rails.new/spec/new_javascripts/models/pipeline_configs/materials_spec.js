/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define([
  'mithril', 'lodash', "models/pipeline_configs/materials", "models/pipeline_configs/scms", 'models/pipeline_configs/plugin_infos'
], function (m, _, Materials, SCMs, PluginInfos) {
  var materials, gitMaterial, svnMaterial, mercurialMaterial, perforceMaterial, tfsMaterial, dependencyMaterial;
  beforeAll(function () {
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


  describe("Material Model", function () {
    describe("validation", function () {
      it("should not allow materials with duplicate names", function () {
        var errorsOnOriginal = gitMaterial.validate();
        expect(errorsOnOriginal._isEmpty()).toBe(true);

        var duplicate = materials.createMaterial({
          type: 'git',
          name: gitMaterial.name()
        });

        errorsOnOriginal = gitMaterial.validate();
        expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

        var errorsOnDuplicate = duplicate.validate();
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
      });

      it("should allow multiple materials to have blank names", function () {
        var materialA = materials.createMaterial({
          type: 'git',
          name: '',
          url:  'https://github.com/gocd/gocd'
        });

        var materialB = materials.createMaterial({
          type: 'git',
          name: '',
          url:  'https://github.com/gocd/website'
        });

        var errorsOnA = materialA.validate();
        expect(errorsOnA.hasErrors('name')).toBe(false);

        var errorsOnB = materialB.validate();
        expect(errorsOnB.hasErrors('name')).toBe(false);
      });
    });

    describe('Test Connection', function () {
      var requestArgs, material;

      beforeEach(function () {
        material = new Materials().createMaterial({
          type: 'git',
          url:  "http://git.example.com/git/myProject"
        });

        spyOn(m, 'request');
        material.testConnection(m.prop('testPipeline'));
        requestArgs = m.request.calls.mostRecent().args[0];
      });

      describe('post', function () {
        it('should post to material_test url', function () {
          expect(requestArgs.method).toBe('POST');
          expect(requestArgs.url).toBe('/go/api/admin/material_test');
        });

        it('should post required headers', function () {
          var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);
          requestArgs.config(xhr);

          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v1+json");
        });

        it('should post the material for test connection', function () {
          var payload = _.merge(material.toJSON(), {pipeline_name: 'testPipeline'}); // eslint-disable-line camelcase

          expect(JSON.stringify(requestArgs.data)).toBe(JSON.stringify(payload));
        });

        it('should return test connection failure message', function () {
          var errorMessage = "Failed to find 'hg' on your PATH";

          expect(requestArgs.unwrapError({message: errorMessage})).toBe(errorMessage);
        });

        it('should stringfy the request payload', function () {
          var payload = {'keyOne': 'value'};

          spyOn(JSON, 'stringify').and.callThrough();

          expect(requestArgs.serialize(payload)).toBe(JSON.stringify({key_one: 'value'})); // eslint-disable-line camelcase
          expect(JSON.stringify).toHaveBeenCalled();
        });
      });
    });
  });

  describe("Material Model", function () {
    describe("SVN", function () {
      it("should initialize material model with type", function () {
        expect(svnMaterial.type()).toBe("svn");
      });

      it("should initialize material model with url", function () {
        expect(svnMaterial.url()).toBe("http://svn.example.com/svn/myProject");
      });

      it("should initialize material model with username", function () {
        expect(svnMaterial.username()).toBe("bob");
      });

      it("should initialize material model with password", function () {
        expect(svnMaterial.passwordValue()).toBe("p@ssw0rd");
      });

      it("should initialize material model with checkExternals", function () {
        expect(svnMaterial.checkExternals()).toBe(true);
      });

      it("should initialize material model with destination", function () {
        expect(svnMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with name", function () {
        expect(svnMaterial.name()).toBe("svn-repo");
      });

      it("should initialize material model with autoUpdate", function () {
        expect(svnMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", function () {
        expect(svnMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with invert_filter", function () {
        expect(svnMaterial.invertFilter()).toBe(true);
      });

      describe("validation", function () {
        it("should add error when url is blank", function () {
          svnMaterial.url("");
          var errors = svnMaterial.validate();
          expect(errors.errors('url')).toEqual(['URL must be present']);
        });
      });

      describe("Deserialization from JSON", function () {
        beforeEach(function () {
          svnMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should initialize from json", function () {
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

        it('should map server side errors', function () {
          var material = Materials.Material.fromJSON({
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

    describe("Git", function () {
      it("should initialize material model with type", function () {
        expect(gitMaterial.type()).toBe("git");
      });

      it("should initialize material model with url", function () {
        expect(gitMaterial.url()).toBe("http://git.example.com/git/myProject");
      });

      it("should initialize material model with branch", function () {
        expect(gitMaterial.branch()).toBe("release-1.2");
      });

      it("should initialize material model with destination", function () {
        expect(gitMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with name", function () {
        expect(gitMaterial.name()).toBe("git-repo");
      });

      it("should initialize material model with autoUpdate", function () {
        expect(gitMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", function () {
        expect(gitMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with shallow clone", function () {
        expect(gitMaterial.shallowClone()).toBe(true);
      });

      it("should initialize material model with invert_filter", function () {
        expect(gitMaterial.invertFilter()).toBe(true);
      });

      describe("validation", function () {
        it("should add error when url is blank", function () {
          gitMaterial.url("");
          var errors = gitMaterial.validate();
          expect(errors.errors('url')).toEqual(['URL must be present']);
        });
      });

      describe("Default Value", function () {
        beforeEach(function () {
          gitMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should use the default branch value when not provided", function () {
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

      describe("Deserialization from JSON", function () {
        beforeEach(function () {
          gitMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should initialize from json", function () {
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

        it('should map server side errors', function () {
          var material = Materials.Material.fromJSON({
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

    describe("Mercurial", function () {
      it("should initialize material model with type", function () {
        expect(mercurialMaterial.type()).toBe("hg");
      });

      it("should initialize material model with url", function () {
        expect(mercurialMaterial.url()).toBe("http://hg.example.com/hg/myProject");
      });

      it("should initialize material model with branch", function () {
        expect(mercurialMaterial.branch()).toBe("release-1.2");
      });

      it("should initialize material model with destination", function () {
        expect(mercurialMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with name", function () {
        expect(mercurialMaterial.name()).toBe("hg-repo");
      });

      it("should initialize material model with autoUpdate", function () {
        expect(mercurialMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", function () {
        expect(mercurialMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with invert_filter", function () {
        expect(mercurialMaterial.invertFilter()).toBe(true);
      });

      describe("validation", function () {
        it("should add error when url is blank", function () {
          mercurialMaterial.url("");
          var errors = mercurialMaterial.validate();
          expect(errors.errors('url')).toEqual(['URL must be present']);
        });
      });

      describe("Deserialization from JSON", function () {
        beforeEach(function () {
          mercurialMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should initialize from json", function () {
          expect(mercurialMaterial.type()).toBe("hg");
          expect(mercurialMaterial.url()).toBe("http://hg.example.com/hg/myProject");
          expect(mercurialMaterial.branch()).toBe('release-1.2');
          expect(mercurialMaterial.destination()).toBe("projectA");
          expect(mercurialMaterial.name()).toBe("materialA");
          expect(mercurialMaterial.autoUpdate()).toBe(true);
          expect(mercurialMaterial.filter().ignore()).toEqual(['*.doc']);
          expect(mercurialMaterial.invertFilter()).toBe(true);
        });

        it('should map server side errors', function () {
          var material = Materials.Material.fromJSON({
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

    describe("Perforce", function () {
      it("should initialize material model with type", function () {
        expect(perforceMaterial.type()).toBe("p4");
      });

      it("should initialize material model with port", function () {
        expect(perforceMaterial.port()).toBe("p4.example.com:1666");
      });

      it("should initialize material model with username", function () {
        expect(perforceMaterial.username()).toBe("bob");
      });

      it("should initialize material model with password", function () {
        expect(perforceMaterial.passwordValue()).toBe("p@ssw0rd");
      });

      it("should initialize material model with useTickets", function () {
        expect(perforceMaterial.useTickets()).toBe(true);
      });

      it("should initialize material model with destination", function () {
        expect(perforceMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with view", function () {
        expect(perforceMaterial.view()).toBe("//depot/dev/source...          //anything/source/");
      });

      it("should initialize material model with name", function () {
        expect(perforceMaterial.name()).toBe("perforce-repo");
      });

      it("should initialize material model with autoUpdate", function () {
        expect(perforceMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", function () {
        expect(perforceMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with invert_filter", function () {
        expect(perforceMaterial.invertFilter()).toBe(true);
      });

      describe("validation", function () {
        it("should add error when port is blank", function () {
          perforceMaterial.port("");
          var errors = perforceMaterial.validate();
          expect(errors.errors('port')).toEqual(['Port must be present']);
        });

        it("should add error when view is blank", function () {
          perforceMaterial.view("");
          var errors = perforceMaterial.validate();
          expect(errors.errors('view')).toEqual(['View must be present']);
        });
      });

      describe("Deserialization from JSON", function () {
        beforeEach(function () {
          perforceMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should initialize from json", function () {
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

        it('should map server side errors', function () {
          var material = Materials.Material.fromJSON({
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

    describe("TFS", function () {
      it("should initialize material model with type", function () {
        expect(tfsMaterial.type()).toBe("tfs");
      });

      it("should initialize material model with url", function () {
        expect(tfsMaterial.url()).toBe("http://tfs.example.com/tfs/projectA");
      });

      it("should initialize material model with username", function () {
        expect(tfsMaterial.username()).toBe("bob");
      });

      it("should initialize material model with password", function () {
        expect(tfsMaterial.passwordValue()).toBe("p@ssw0rd");
      });

      it("should initialize material model with domain", function () {
        expect(tfsMaterial.domain()).toBe('AcmeCorp');
      });

      it("should initialize material model with destination", function () {
        expect(tfsMaterial.destination()).toBe("projectA");
      });

      it("should initialize material model with projectPath", function () {
        expect(tfsMaterial.projectPath()).toBe("$/webApp");
      });

      it("should initialize material model with name", function () {
        expect(tfsMaterial.name()).toBe("tfs-repo");
      });

      it("should initialize material model with autoUpdate", function () {
        expect(tfsMaterial.autoUpdate()).toBe(true);
      });

      it("should initialize material model with filters", function () {
        expect(tfsMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with invert_filter", function () {
        expect(tfsMaterial.invertFilter()).toBe(true);
      });

      describe("validation", function () {
        it("should add error when url is blank", function () {
          tfsMaterial.url("");
          var errors = tfsMaterial.validate();
          expect(errors.errors('url')).toEqual(['URL must be present']);
        });

        it("should add error when username is blank", function () {
          tfsMaterial.username("");
          var errors = tfsMaterial.validate();
          expect(errors.errors('username')).toEqual(['Username must be present']);
        });

        it("should add error when projectPath is blank", function () {
          tfsMaterial.projectPath("");
          var errors = tfsMaterial.validate();
          expect(errors.errors('projectPath')).toEqual(['Project path must be present']);
        });
      });

      describe("Serialization/De-serialization to/from JSON", function () {
        beforeEach(function () {
          tfsMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should serialize to JSON", function () {
          expect(tfsMaterial.toJSON()).toEqual(sampleJSON());
        });

        it("should de-serialize from JSON", function () {
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

        it('should map server side errors', function () {
          var material = Materials.Material.fromJSON({
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

    describe('dependency', function () {
      it('it should initialize material with type', function () {
        expect(dependencyMaterial.type()).toBe('dependency');
      });

      it('it should initialize material with pipeline', function () {
        expect(dependencyMaterial.pipeline()).toBe('p1');
      });

      it('it should initialize material with stage', function () {
        expect(dependencyMaterial.stage()).toBe('first_stage');
      });

      it('it should initialize material with name', function () {
        expect(dependencyMaterial.name()).toBe('p1_first_stage');
      });

      describe("validation", function () {
        var errors;
        beforeAll(function () {
          dependencyMaterial.pipeline('');
          dependencyMaterial.stage('');
          errors = dependencyMaterial.validate();
        });

        it("should check presence of pipeline", function () {
          expect(errors.errors('pipeline')).toEqual(['Pipeline must be present']);
        });

        it("should check presence of stage", function () {
          expect(errors.errors('stage')).toEqual(['Stage must be present']);
        });
      });

      describe("Serialization/De-serialization to/from JSON", function () {
        beforeEach(function () {
          dependencyMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should serialize to JSON", function () {
          expect(dependencyMaterial.toJSON()).toEqual(sampleJSON());
        });

        it("should de-serialize from JSON", function () {
          expect(dependencyMaterial.type()).toBe("dependency");
          expect(dependencyMaterial.name()).toBe("materialA");
          expect(dependencyMaterial.pipeline()).toBe('p1');
          expect(dependencyMaterial.stage()).toEqual('s1');
        });

        it('should map server side errors', function () {
          var material = Materials.Material.fromJSON({
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

    describe('plugin', function () {
      var pluggableMaterial;
      var github = new SCMs.SCM({
        /* eslint-disable camelcase */
        id:              '43c45e0b-1b0c-46f3-a60a-2bbc5cec069c',
        name:            'Github PR',
        auto_update:     true,
        plugin_metadata: {id: 'github.pr', version: '1.1'},
        configuration:   [{key: 'url', value: 'path/to/repo'}, {key: 'username', value: 'some_name'}]
        /* eslint-enable camelcase */
      });


      beforeAll(function () {
        SCMs([github]);
        pluggableMaterial = Materials.create({
          type:         "plugin",
          scm:          github,
          filter:       new Materials.Filter({ignore: ['*.doc']}),
          destination:  "dest_folder",
          invertFilter: true
        });
        spyOn(SCMs, 'findById').and.returnValue(github);
      });

      afterAll(function () {
        SCMs([]);
      });

      it('it should initialize material with type', function () {
        expect(pluggableMaterial.type()).toBe('plugin');
      });

      it('it should initialize material with scm', function () {
        expect(pluggableMaterial.scm().id()).toBe('43c45e0b-1b0c-46f3-a60a-2bbc5cec069c');
      });

      it("should initialize material model with filters", function () {
        expect(pluggableMaterial.filter().ignore()).toEqual(['*.doc']);
      });

      it("should initialize material model with destination", function () {
        expect(pluggableMaterial.destination()).toBe('dest_folder');
      });

      it("should initialize material model with pluginInfo", function () {
        expect(Materials.create({pluginInfo: new PluginInfos.PluginInfo({id: 'plugin_id'})}).pluginInfo().id()).toBe('plugin_id');
      });

      it("should initialize material model with invert_filter", function () {
        expect(pluggableMaterial.invertFilter()).toBe(true);
      });

      describe("Serialization/De-serialization to/from JSON", function () {
        beforeEach(function () {
          pluggableMaterial = Materials.Material.fromJSON(sampleJSON());
        });

        it("should serialize to JSON", function () {
          expect(pluggableMaterial.toJSON()).toEqual(sampleJSON());
          expect(SCMs.findById).toHaveBeenCalledWith('43c45e0b-1b0c-46f3-a60a-2bbc5cec069c');
        });

        it("should de-serialize from JSON", function () {
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
