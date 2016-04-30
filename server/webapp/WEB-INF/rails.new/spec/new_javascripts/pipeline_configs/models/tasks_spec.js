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

define(['lodash', "pipeline_configs/models/tasks", "string-plus"], function (_, Tasks, s) {
  describe("Task Model", function () {
    var task;
    describe("Ant", function () {
      beforeEach(function () {
        task = new Tasks.Task.Ant({
          buildFile:        'build-moduleA.xml',
          target:           "clean",
          workingDirectory: "moduleA"
        });
      });

      it("should initialize task model with type", function () {
        expect(task.type()).toBe("ant");
      });

      it("should initialize task model with target", function () {
        expect(task.target()).toBe("clean");
      });

      it("should initialize task model with workingDirectory", function () {
        expect(task.workingDirectory()).toBe("moduleA");
      });

      it("should initialize task model with buildFile", function () {
        expect(task.buildFile()).toBe("build-moduleA.xml");
      });

      it('should have a string representation', function () {
        expect(task.toString()).toBe('clean build-moduleA.xml');
      });

      describe("Serialization from/to JSON", function () {
        beforeEach(function () {
          task = Tasks.Task.fromJSON(sampleTaskJSON());
        });

        it("should de-serialize from JSON", function () {
          expect(task.type()).toBe("ant");
          expect(task.target()).toBe('clean');
          expect(task.workingDirectory()).toBe("moduleA");
        });

        it("should de-serialize and map errors on plain variables", function () {
          expect(task.errors().errorsForDisplay('buildFile')).toBe('error message for build file.');
          expect(task.errors().errorsForDisplay('target')).toBe('error message for target.');
          expect(task.errors().errorsForDisplay('workingDirectory')).toBe('error message for working directory.');
        });

        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        });

        function sampleTaskJSON() {
          return {
            type:       "ant",
            attributes: {
              build_file:        'build-moduleA.xml',
              target:            'clean',
              working_directory: "moduleA"
            },
            errors:     {
              target:            ["error message for target"],
              build_file:        ["error message for build file"],
              working_directory: ["error message for working directory"]
            }
          };
        }
      });
    });

    describe("NAnt", function () {
      beforeEach(function () {
        task = new Tasks.Task.NAnt({
          buildFile:        'build-moduleA.xml',
          target:           "clean",
          workingDirectory: "moduleA",
          nantPath:         'C:\\NAnt'
        });
      });

      it("should initialize task model with type", function () {
        expect(task.type()).toBe("nant");
      });

      it("should initialize task model with target", function () {
        expect(task.target()).toBe("clean");
      });

      it("should initialize task model with workingDirectory", function () {
        expect(task.workingDirectory()).toBe("moduleA");
      });

      it("should initialize task model with buildFile", function () {
        expect(task.buildFile()).toBe("build-moduleA.xml");
      });
      it("should initialize task model with nantPath", function () {
        expect(task.nantPath()).toBe("C:\\NAnt");
      });

      it('should have a string representation', function () {
        expect(task.toString()).toBe('clean build-moduleA.xml');
      });

      describe("Serialize from/to JSON", function () {
        beforeEach(function () {
          task = Tasks.Task.fromJSON(sampleTaskJSON());
        });

        it("should de-serialize from JSON", function () {
          expect(task.type()).toBe("nant");
          expect(task.target()).toBe('clean');
          expect(task.workingDirectory()).toBe("moduleA");
          expect(task.nantPath()).toBe("C:\\NAnt");
        });

        it("should de-serialize and map errors on plain variables", function () {
          expect(task.errors().errorsForDisplay('buildFile')).toBe('error message for build file.');
          expect(task.errors().errorsForDisplay('target')).toBe('error message for target.');
          expect(task.errors().errorsForDisplay('workingDirectory')).toBe('error message for working directory.');
          expect(task.errors().errorsForDisplay('nantPath')).toBe('error message for working nant_path.');
        });

        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        })

        function sampleTaskJSON() {
          return {
            type:       "nant",
            attributes: {
              build_file:        'build-moduleA.xml',
              target:            'clean',
              working_directory: "moduleA",
              nant_path:         "C:\\NAnt"
            },
            errors:     {
              target:            ["error message for target"],
              build_file:        ["error message for build file"],
              working_directory: ["error message for working directory"],
              nant_path:         ["error message for working nant_path"]
            }
          };
        }
      });
    });

    describe("Exec", function () {
      describe('initialize', function () {
        var taskJSON, task;
        beforeEach(function(){
          taskJSON = {
            command:          'bash',
            workingDirectory: 'moduleA'
          };

          task = new Tasks.Task.Exec(taskJSON);
        });

        it("should initialize task model with command", function () {
          expect(task.type()).toBe("exec");
        });

        it("should initialize task model with workingDirectory", function () {
          expect(task.workingDirectory()).toBe("moduleA");
        });

        it("should initialize task model with args as list", function () {
          taskJSON['arguments'] = ['-c', 'ls -al /'];

          var task = new Tasks.Task.Exec(taskJSON);

          expect(task.args().data()).toEqual(['-c', 'ls -al /']);
        });

        it("should initialize task model with args as string", function () {
          taskJSON['args'] = '-a';

          var task = new Tasks.Task.Exec(taskJSON);

          expect(task.args().data()).toEqual('-a');
        });

        it('should have a string representation', function () {
          taskJSON['args'] = '-a';
          var task = new Tasks.Task.Exec(taskJSON);

          expect(task.toString()).toBe("bash -a");
        });
      });


      describe("Serialize from/to JSON", function () {
        beforeEach(function () {
          task = Tasks.Task.fromJSON(sampleTaskJSON());
        });

        it("should de-serialize from JSON", function () {
          expect(task.type()).toBe("exec");
          expect(task.command()).toBe('bash');
          expect(task.args().data()).toEqual(['-c', 'ls -al /']);
        });

        it("should de-serialize and map errors on plain variables", function () {
          expect(task.errors().errorsForDisplay('command')).toBe('error message for command.');
          expect(task.errors().errorsForDisplay('workingDirectory')).toBe('error message for working directory.');
        });


        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        });

        function sampleTaskJSON() {
          return {
            type:       "exec",
            attributes: {
              command:           'bash',
              arguments:         ['-c', 'ls -al /'],
              working_directory: "moduleA"
            },
            errors:     {
              command:           ["error message for command"],
              working_directory: ["error message for working directory"]
            }

          };
        }
      });
    });

    describe("Rake", function () {
      beforeEach(function () {
        task = new Tasks.Task.Rake({
          buildFile:        'foo.rake',
          target:           "clean",
          workingDirectory: "moduleA"
        });
      });

      it("should initialize task model with type", function () {
        expect(task.type()).toBe("rake");
      });

      it("should initialize task model with target", function () {
        expect(task.target()).toBe("clean");
      });

      it("should initialize task model with workingDirectory", function () {
        expect(task.workingDirectory()).toBe("moduleA");
      });

      it("should initialize task model with buildFile", function () {
        expect(task.buildFile()).toBe("foo.rake");
      });

      it('should have a string representation', function () {
        expect(task.toString()).toBe('clean foo.rake');
      });

      describe("Serialize from/to JSON", function () {
        beforeEach(function () {
          task = Tasks.Task.fromJSON(sampleTaskJSON());
        });

        it("should de-serialize from json", function () {
          expect(task.type()).toBe("rake");
          expect(task.target()).toBe('clean');
          expect(task.workingDirectory()).toBe("moduleA");
        });

        it("should de-serialize and map errors on plain variables", function () {
          expect(task.errors().errorsForDisplay('buildFile')).toBe('error message for build file.');
          expect(task.errors().errorsForDisplay('target')).toBe('error message for target.');
          expect(task.errors().errorsForDisplay('workingDirectory')).toBe('error message for working directory.');
        });


        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        });

        function sampleTaskJSON() {
          return {
            type:       "rake",
            attributes: {
              build_file:        'foo.rake',
              target:            'clean',
              working_directory: "moduleA"
            },
            errors:     {
              target:            ["error message for target"],
              build_file:        ["error message for build file"],
              working_directory: ["error message for working directory"]
            }
          };
        }
      });
    });

    describe("FetchArtifact", function () {
      beforeEach(function () {
        task = new Tasks.Task.FetchArtifact({
          pipeline: 'Build',
          stage:    "Dist",
          job:      "RPM",
          source:   new Tasks.Task.FetchArtifact.Source({type: 'dir', location: 'pkg'})
        });
      });

      it("should initialize task model with pipeline", function () {
        expect(task.pipeline()).toBe("Build");
      });

      it("should initialize task model with stage", function () {
        expect(task.stage()).toBe("Dist");
      });

      it("should initialize task model with job", function () {
        expect(task.job()).toBe("RPM");
      });

      it("should initialize task model with source", function () {
        expect(task.source().type()).toBe("dir");
        expect(task.source().location()).toBe("pkg");
      });

      it('should have a string representation', function () {
        expect(task.toString()).toBe('Build Dist RPM');
      });

      describe("Serialize from/to JSON", function () {
        beforeEach(function () {
          task = Tasks.Task.fromJSON(sampleTaskJSON());
        });

        it("should de-serialize from JSON", function () {
          expect(task.type()).toBe("fetchartifact");
          expect(task.pipeline()).toBe("Build");
          expect(task.stage()).toBe("Dist");
          expect(task.job()).toBe("RPM");
          expect(task.source().type()).toBe("dir");
          expect(task.source().location()).toBe("pkg");
        });

        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        });

        function sampleTaskJSON() {
          return {
            type:       "fetchartifact",
            attributes: {
              pipeline: 'Build',
              stage:    "Dist",
              job:      "RPM",
              source:   {
                type:     'dir',
                location: 'pkg'
              }
            }
          };
        }
      });
    });

    describe("Plugin Task", function () {
      beforeEach(function () {
        task = new Tasks.Task.PluginTask({
          pluginId:      'indix.s3fetch',
          version:       1,
          configuration: Tasks.Task.PluginTask.Configurations.fromJSON([
            {name: "Repo", value: "foo"},
            {name: "Package", value: "foobar-widgets"}
          ])
        });
      });

      it("should initialize task model with type", function () {
        expect(task.type()).toBe("plugin");
      });

      it("should initialize task model with pluginId", function () {
        expect(task.pluginId()).toBe("indix.s3fetch");
      });

      it("should initialize task model with version", function () {
        expect(task.version()).toBe(1);
      });

      it("should initialize task model with configuration", function () {
        expect(task.configuration().collectConfigurationProperty('name')).toEqual(['Repo', 'Package']);
        expect(task.configuration().collectConfigurationProperty('value')).toEqual(['foo', 'foobar-widgets']);
      });

      describe("Serialize from/to JSON", function () {
        beforeEach(function () {
          var data = sampleTaskJSON();
          task     = Tasks.Task.fromJSON(data);
        });

        it("should de-serialize from json", function () {
          expect(task.type()).toBe("plugin");
          expect(task.pluginId()).toBe("indix.s3fetch");
          expect(task.version()).toBe(1);

          expect(task.configuration().collectConfigurationProperty('name')).toEqual(['Repo', 'Package']);
          expect(task.configuration().collectConfigurationProperty('value')).toEqual(['foo', 'foobar-widgets']);
        });

        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        });

        function sampleTaskJSON() {
          return {
            type:       "plugin",
            attributes: {
              plugin_id:     "indix.s3fetch",
              version:       1,
              configuration: [
                {name: "Repo", value: "foo"},
                {name: "Package", value: "foobar-widgets"}
              ]
            }
          };
        }
      });
    });

  });
});
