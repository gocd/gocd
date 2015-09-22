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

define(['lodash', "pipeline_configs/models/materials"], function (_, Materials) {
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
  });


  describe("Materials Model", function () {
    describe("validation", function () {
      it("should not allow materials with duplicate names", function () {
        var errorsOnOriginal = gitMaterial.validate();
        expect(errorsOnOriginal._isEmpty()).toBe(true);

        var duplicate = materials.createMaterial({
          type: 'git',
          name: gitMaterial.name()
        });

        var errorsOnOriginal = gitMaterial.validate();
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
        expect(errorsOnA._isEmpty()).toBe(true);

        var errorsOnB = materialB.validate();
        expect(errorsOnB._isEmpty()).toBe(true);
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
        expect(svnMaterial.password()).toBe("p@ssw0rd");
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
          expect(svnMaterial.password()).toBe("p@ssw0rd");
          expect(svnMaterial.checkExternals()).toBe(true);
          expect(svnMaterial.destination()).toBe("projectA");
          expect(svnMaterial.name()).toBe("materialA");
          expect(svnMaterial.autoUpdate()).toBe(true);
          expect(svnMaterial.filter().ignore()).toEqual(['*.doc']);
        });

        function sampleJSON() {
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
              }
            }
          };
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

      describe("validation", function () {
        it("should add error when url is blank", function () {
          gitMaterial.url("");
          var errors = gitMaterial.validate();
          expect(errors.errors('url')).toEqual(['URL must be present']);
        });
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
          expect(gitMaterial.filter().ignore()).toEqual(['*.doc'])
        });

        function sampleJSON() {
          return {
            type:       "git",
            attributes: {
              url:         "http://git.example.com/git/myProject",
              branch:      "release-1.2",
              destination: "projectA",
              name:        "materialA",
              auto_update: true,
              filter:      {
                ignore: ['*.doc']
              }
            }
          };
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
          expect(mercurialMaterial.filter().ignore()).toEqual(['*.doc'])
        });

        function sampleJSON() {
          return {
            type:       "hg",
            attributes: {
              url:         "http://hg.example.com/hg/myProject",
              branch:      "release-1.2",
              destination: "projectA",
              name:        "materialA",
              auto_update: true,
              filter:      {
                ignore: ['*.doc']
              }
            }
          };
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
        expect(perforceMaterial.password()).toBe("p@ssw0rd");
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
          expect(perforceMaterial.password()).toBe("p@ssw0rd");
          expect(perforceMaterial.useTickets()).toBe(true);
          expect(perforceMaterial.destination()).toBe("projectA");
          expect(perforceMaterial.name()).toBe("materialA");
          expect(perforceMaterial.autoUpdate()).toBe(true);
          expect(perforceMaterial.view()).toBe("//depot/dev/source...          //anything/source/");
          expect(perforceMaterial.filter().ignore()).toEqual(['*.doc'])
        });

        function sampleJSON() {
          return {
            type:       "p4",
            attributes: {
              port:        "p4.example.com:1666",
              username:    "bob",
              password:    "p@ssw0rd",
              use_tickets: true,
              destination: "projectA",
              view:        "//depot/dev/source...          //anything/source/",
              name:        "materialA",
              auto_update: true,
              filter:      {
                ignore: ['*.doc']
              }
            }
          };
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
        expect(tfsMaterial.password()).toBe("p@ssw0rd");
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
          tfsMaterial = Materials.Material.fromJSON(sampleTaskJSON());
        });

        it("should serialize to JSON", function () {
          expect(tfsMaterial.toJSON()).toEqual(sampleTaskJSON());
        });

        it("should de-serialize from JSON", function () {
          expect(tfsMaterial.type()).toBe("tfs");
          expect(tfsMaterial.url()).toBe("http://tfs.example.com/tfs/projectA");
          expect(tfsMaterial.username()).toBe("bob");
          expect(tfsMaterial.password()).toBe("p@ssw0rd");
          expect(tfsMaterial.domain()).toBe('AcmeCorp');
          expect(tfsMaterial.destination()).toBe("projectA");
          expect(tfsMaterial.name()).toBe("materialA");
          expect(tfsMaterial.autoUpdate()).toBe(true);
          expect(tfsMaterial.projectPath()).toBe("$/webApp");
          expect(tfsMaterial.filter().ignore()).toEqual(['*.doc']);
        });

        function sampleTaskJSON() {
          return {
            type:       "tfs",
            attributes: {
              name:         "materialA",
              auto_update:  true,
              filter:       {
                ignore: ['*.doc']
              },
              url:          "http://tfs.example.com/tfs/projectA",
              username:     "bob",
              password:     "p@ssw0rd",
              domain:       'AcmeCorp',
              destination:  "projectA",
              project_path: "$/webApp"
            }
          };
        }
      });
    });

  });
});
