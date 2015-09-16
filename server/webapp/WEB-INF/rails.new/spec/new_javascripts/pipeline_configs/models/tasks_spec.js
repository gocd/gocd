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

define(['lodash', "pipeline_configs/models/tasks", "string-plus"], function (_, Tasks, s) {
  describe("Task Model", function () {
    var task;
    describe("Ant", function () {
      beforeEach(function () {
        task = new Tasks.Task.Ant({
          buildFile:  'build-moduleA.xml',
          target:     "clean",
          workingDir: "moduleA"
        });
      });

      it("should initialize task model with type", function () {
        expect(task.type()).toBe("ant");
      });

      it("should initialize task model with target", function () {
        expect(task.target()).toBe("clean");
      });

      it("should initialize task model with workingDir", function () {
        expect(task.workingDir()).toBe("moduleA");
      });

      it("should initialize task model with buildFile", function () {
        expect(task.buildFile()).toBe("build-moduleA.xml");
      });

      describe("Serialization from/to JSON", function () {
        beforeEach(function () {
          task = Tasks.Task.fromJSON(sampleTaskJSON());
        });

        it("should de-serialize from JSON", function () {
          expect(task.type()).toBe("ant");
          expect(task.target()).toBe('clean');
          expect(task.workingDir()).toBe("moduleA");
        });

        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        });

        function sampleTaskJSON() {
          return {
            type:       "ant",
            attributes: {
              build_file:  'build-moduleA.xml',
              target:      'clean',
              working_dir: "moduleA"
            }
          };
        }
      });
    });

    describe("NAnt", function () {
      beforeEach(function () {
        task = new Tasks.Task.NAnt({
          buildFile:  'build-moduleA.xml',
          target:     "clean",
          workingDir: "moduleA",
          nantHome:   'C:\\NAnt'
        });
      });

      it("should initialize task model with type", function () {
        expect(task.type()).toBe("nant");
      });

      it("should initialize task model with target", function () {
        expect(task.target()).toBe("clean");
      });

      it("should initialize task model with workingDir", function () {
        expect(task.workingDir()).toBe("moduleA");
      });

      it("should initialize task model with buildFile", function () {
        expect(task.buildFile()).toBe("build-moduleA.xml");
      });
      it("should initialize task model with nantHome", function () {
        expect(task.nantHome()).toBe("C:\\NAnt");
      });

      describe("Serialize from/to JSON", function () {
        beforeEach(function () {
          task = Tasks.Task.fromJSON(sampleTaskJSON());
        });

        it("should de-serialize from JSON", function () {
          expect(task.type()).toBe("nant");
          expect(task.target()).toBe('clean');
          expect(task.workingDir()).toBe("moduleA");
          expect(task.nantHome()).toBe("C:\\NAnt");
        });

        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        })

        function sampleTaskJSON() {
          return {
            type:       "nant",
            attributes: {
              build_file:  'build-moduleA.xml',
              target:      'clean',
              working_dir: "moduleA",
              nant_home:   "C:\\NAnt"
            }
          };
        }
      });
    });

    describe("Exec", function () {
      beforeEach(function () {
        task = new Tasks.Task.Exec({
          command:    'bash',
          args:       ['-c', 'ls -al /'],
          workingDir: "moduleA"
        });
      });

      it("should initialize task model with command", function () {
        expect(task.type()).toBe("exec");
      });

      it("should initialize task model with args", function () {
        expect(task.args()).toEqual(['-c', 'ls -al /']);
      });

      it("should initialize task model with workingDir", function () {
        expect(task.workingDir()).toBe("moduleA");
      });

      describe("Serialize from/to JSON", function () {
        beforeEach(function () {
          task = Tasks.Task.fromJSON(sampleTaskJSON());
        });

        it("should de-serialize from JSON", function () {
          expect(task.type()).toBe("exec");
          expect(task.command()).toBe('bash');
          expect(task.args()).toEqual(['-c', 'ls -al /']);
        });

        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        });

        function sampleTaskJSON() {
          return {
            type:       "exec",
            attributes: {
              command:     'bash',
              args:        ['-c', 'ls -al /'],
              working_dir: "moduleA"
            }
          };
        }
      });
    });

    describe("Rake", function () {
      beforeEach(function () {
        task = new Tasks.Task.Rake({
          buildFile:  'foo.rake',
          target:     "clean",
          workingDir: "moduleA"
        });
      });

      it("should initialize task model with type", function () {
        expect(task.type()).toBe("rake");
      });

      it("should initialize task model with target", function () {
        expect(task.target()).toBe("clean");
      });

      it("should initialize task model with workingDir", function () {
        expect(task.workingDir()).toBe("moduleA");
      });

      it("should initialize task model with buildFile", function () {
        expect(task.buildFile()).toBe("foo.rake");
      });

      describe("Serialize from/to JSON", function () {
        beforeEach(function () {
          task = Tasks.Task.fromJSON(sampleTaskJSON());
        });

        it("should de-serialize from json", function () {
          expect(task.type()).toBe("rake");
          expect(task.target()).toBe('clean');
          expect(task.workingDir()).toBe("moduleA");
        });

        it("should serialize to JSON", function () {
          expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
        });

        function sampleTaskJSON() {
          return {
            type:       "rake",
            attributes: {
              build_file:  'foo.rake',
              target:      'clean',
              working_dir: "moduleA"
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
