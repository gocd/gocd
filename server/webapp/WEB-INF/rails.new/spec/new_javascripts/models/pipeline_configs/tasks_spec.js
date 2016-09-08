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

define(['lodash', "models/pipeline_configs/tasks", "string-plus", 'models/pipeline_configs/plugin_infos'],
  function (_, Tasks, s, PluginInfos) {
    describe("Task Model", function () {
      var task;
      describe("Ant", function () {
        beforeAll(function () {
          task = new Tasks.Task.Ant({
            buildFile:        'build-moduleA.xml',
            target:           "clean",
            workingDirectory: "moduleA",
            runIf:            ['any']
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

        it("should initialize task model with runIfConditions", function () {
          expect(task.runIf().data()).toEqual(['any']);
        });

        it("should initialize task model with buildFile", function () {
          expect(task.buildFile()).toBe("build-moduleA.xml");
        });

        it('should have a string representation', function () {
          expect(task.toString()).toBe('clean build-moduleA.xml');
        });

        it('should initialize onCancel task', function() {
          var cancelTask = 'cancelTask';
          spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

          var task = new Tasks.Task.Ant({
            onCancelTask: {type: 'Nant'}
          });

          expect(task.onCancelTask).toBe(cancelTask);
        });

        it('should not have onCancel task if not specified', function(){
          var task = new Tasks.Task.Ant({
            onCancelTask: null
          });

          expect(task.onCancelTask).toBe(null);
        });

        describe("Serialization from/to JSON", function () {
          beforeEach(function () {
            task = Tasks.Task.fromJSON(sampleTaskJSON());
          });

          it("should de-serialize from JSON", function () {
            expect(task.type()).toBe("ant");
            expect(task.target()).toBe('clean');
            expect(task.workingDirectory()).toBe("moduleA");
            expect(task.runIf().data()).toEqual(['any']);
            expect(task.onCancelTask.type()).toBe('nant');
          });

          it("should serialize to JSON", function () {
            expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
          });

          function sampleTaskJSON() {
          /* eslint-disable camelcase */
            return {
              type:       "ant",
              attributes: {
                build_file:        'build-moduleA.xml',
                target:            'clean',
                working_directory: "moduleA",
                run_if:            ['any'],
                on_cancel: {
                  type:       "nant",
                  attributes: {
                    build_file:        'build-moduleA.xml',
                    target:            'clean',
                    working_directory: "moduleA",
                    nant_path:         "C:\\NAnt",
                    run_if:            ['passed'],
                    on_cancel:         null
                  }
                }
              }
            };
          /* eslint-enable camelcase */
          }
        });
      });

      describe("NAnt", function () {
        beforeAll(function () {
          task = new Tasks.Task.NAnt({
            buildFile:        'build-moduleA.xml',
            target:           "clean",
            workingDirectory: "moduleA",
            nantPath:         'C:\\NAnt',
            runIf:            ['any']
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

        it("should initialize task model with runIfConditions", function () {
          expect(task.runIf().data()).toEqual(['any']);
        });

        it('should have a string representation', function () {
          expect(task.toString()).toBe('clean build-moduleA.xml');
        });

        it('should initialize onCancel task', function() {
          var cancelTask = 'cancelTask';
          spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

          var task = new Tasks.Task.NAnt({
            onCancelTask: {type: 'Nant'}
          });

          expect(task.onCancelTask).toBe(cancelTask);
        });

        it('should not have onCancel task if not specified', function(){
          var task = new Tasks.Task.NAnt({
            onCancelTask: null
          });

          expect(task.onCancelTask).toBe(null);
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
            expect(task.runIf().data()).toEqual(['any']);
          });

          it("should serialize to JSON", function () {
            expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
          });

          function sampleTaskJSON() {
          /* eslint-disable camelcase */
            return {
              type:       "nant",
              attributes: {
                build_file:        'build-moduleA.xml',
                target:            'clean',
                working_directory: "moduleA",
                nant_path:         "C:\\NAnt",
                run_if:            ['any'],
                on_cancel: {
                  type:       "nant",
                  attributes: {
                    build_file:        'build-moduleA.xml',
                    target:            'clean',
                    working_directory: "moduleA",
                    nant_path:         "C:\\NAnt",
                    run_if:            ['passed'],
                    on_cancel:         null
                  }
                }
              }
            };
          /* eslint-enable camelcase */
          }
        });
      });

      describe("Exec", function () {
        describe('initialize', function () {
          var taskJSON, task;
          beforeAll(function(){
            taskJSON = {
              command:          'bash',
              workingDirectory: 'moduleA',
              runIf:            ['any']
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

          it("should initialize task model with runIfConditions", function () {
            expect(task.runIf().data()).toEqual(['any']);
          });

          it('should have a string representation', function () {
            taskJSON['args'] = '-a';
            var task = new Tasks.Task.Exec(taskJSON);

            expect(task.toString()).toBe("bash -a");
          });

          it('should initialize onCancel task', function() {
            var cancelTask = 'cancelTask';
            spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

            var task = new Tasks.Task.Exec({
              onCancelTask: {type: 'Nant'}
            });

            expect(task.onCancelTask).toBe(cancelTask);
          });

          it('should not have onCancel task if not specified', function(){
            var task = new Tasks.Task.Exec({
              onCancelTask: null
            });

            expect(task.onCancelTask).toBe(null);
          });
        });

        describe('validation', function () {
          it('should validate presence of command', function () {
            var task = new Tasks.Task.Exec({});

            expect(task.isValid()).toBe(false);
            expect(task.errors().errors('command')).toEqual(['Command must be present']);
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
            expect(task.runIf().data()).toEqual(['any']);
          });

          it('should map server side errors', function () {
            var task = Tasks.Task.fromJSON({
              type:   "exec",
              errors: {
                command: [
                  "Command cannot be empty"
                ]
              }
            });

            expect(task.errors()._isEmpty()).toBe(false);
            expect(task.errors().errors('command')).toEqual(['Command cannot be empty']);
          });

          it("should serialize to JSON", function () {
            expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
          });

          function sampleTaskJSON() {
          /* eslint-disable camelcase */
            return {
              type:       "exec",
              attributes: {
                command:           'bash',
                arguments:         ['-c', 'ls -al /'],
                working_directory: "moduleA",
                run_if:            ['any'],
                on_cancel: {
                  type:       "nant",
                  attributes: {
                    build_file:        'build-moduleA.xml',
                    target:            'clean',
                    working_directory: "moduleA",
                    nant_path:         "C:\\NAnt",
                    run_if:            ['passed'],
                    on_cancel:         null
                  }
                }
              }
            };
          /* eslint-enable camelcase */
          }
        });
      });

      describe("Rake", function () {
        beforeAll(function () {
          task = new Tasks.Task.Rake({
            buildFile:        'foo.rake',
            target:           "clean",
            workingDirectory: "moduleA",
            runIf:            ['any']
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

        it("should initialize task model with runIfConditions", function () {
          expect(task.runIf().data()).toEqual(['any']);
        });

        it('should have a string representation', function () {
          expect(task.toString()).toBe('clean foo.rake');
        });

        it('should initialize onCancel task', function() {
          var cancelTask = 'cancelTask';
          spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

          var task = new Tasks.Task.Rake({
            onCancelTask: {type: 'Nant'}
          });

          expect(task.onCancelTask).toBe(cancelTask);
        });

        it('should not have onCancel task if not specified', function(){
          var task = new Tasks.Task.Rake({
            onCancelTask: null
          });

          expect(task.onCancelTask).toBe(null);
        });
        describe("Serialize from/to JSON", function () {
          beforeEach(function () {
            task = Tasks.Task.fromJSON(sampleTaskJSON());
          });

          it("should de-serialize from json", function () {
            expect(task.type()).toBe("rake");
            expect(task.target()).toBe('clean');
            expect(task.workingDirectory()).toBe("moduleA");
            expect(task.runIf().data()).toEqual(['any']);
          });

          it("should serialize to JSON", function () {
            expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
          });

          function sampleTaskJSON() {
          /* eslint-disable camelcase */
            return {
              type:       "rake",
              attributes: {
                build_file:        'foo.rake',
                target:            'clean',
                working_directory: "moduleA",
                run_if:            ['any'],
                on_cancel: {
                  type:       "nant",
                  attributes: {
                    build_file:        'build-moduleA.xml',
                    target:            'clean',
                    working_directory: "moduleA",
                    nant_path:         "C:\\NAnt",
                    run_if:            ['passed'],
                    on_cancel:         null
                  }
                }
              }
            };
          /* eslint-enable camelcase */
          }
        });
      });

      describe("FetchArtifact", function () {
        beforeAll(function () {
          task = new Tasks.Task.FetchArtifact({
            pipeline: 'Build',
            stage:    "Dist",
            job:      "RPM",
            source:   "dir",
            isSourceAFile: true,
            destination: "Dest",
            runIf:   ['any']
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
          expect(task.source()).toBe("dir");
        });

        it("should initialize task model with isSourceAFile", function () {
          expect(task.isSourceAFile()).toBe(true);
        });

        it("should initialize task model with Destination", function () {
          expect(task.destination()).toBe("Dest");
        });

        it('should initialize isSourceAFile to be false if not specified', function(){
          var task = new Tasks.Task.FetchArtifact({

          });
          expect(task.isSourceAFile()).toBe(false);
        });

        it("should initialize task model with runIfConditions", function () {
          expect(task.runIf().data()).toEqual(['any']);
        });

        it('should have a string representation', function () {
          expect(task.toString()).toBe('Build Dist RPM');
        });

        it('should initialize onCancel task', function() {
          var cancelTask = 'cancelTask';
          spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

          var task = new Tasks.Task.FetchArtifact({
            onCancelTask: {type: 'Nant'}
          });

          expect(task.onCancelTask).toBe(cancelTask);
        });

        it('should not have onCancel task if not specified', function(){
          var task = new Tasks.Task.FetchArtifact({
            onCancelTask: null
          });

          expect(task.onCancelTask).toBe(null);
        });

        it('should validate presence of required fields', function () {
          var task = new Tasks.Task.FetchArtifact({});

          expect(task.isValid()).toBe(false);
          expect(task.errors().errors('stage')).toEqual(['Stage must be present']);
          expect(task.errors().errors('job')).toEqual(['Job must be present']);
          expect(task.errors().errors('source')).toEqual(['Source must be present']);
        });

        describe("Serialize from/to JSON", function () {
          beforeEach(function () {
            task = Tasks.Task.fromJSON(sampleTaskJSON());
          });

          it("should de-serialize from JSON", function () {
            expect(task.type()).toBe("fetch");
            expect(task.pipeline()).toBe("Build");
            expect(task.stage()).toBe("Dist");
            expect(task.job()).toBe("RPM");
            expect(task.source()).toBe("dir");
            expect(task.isSourceAFile()).toBe(true);
            expect(task.destination()).toBe("Dest");
            expect(task.runIf().data()).toEqual(['any']);
          });

          it('should map server side errors', function () {
            var task = Tasks.Task.fromJSON({
              type: "fetch",
              errors: {
                job: [
                  'Job is a required field.'
                ],
                source: [
                  'Should provide either srcdir or srcfile'
                ],
                stage: [
                  'Stage is a required field.'
                ]
              }
            });

            expect(task.errors()._isEmpty()).toBe(false);
            expect(task.errors().errors('stage')).toEqual(['Stage is a required field.']);
            expect(task.errors().errors('job')).toEqual(['Job is a required field.']);
            expect(task.errors().errors('source')).toEqual(['Should provide either srcdir or srcfile']);
          });

          it("should serialize to JSON", function () {
            expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
          });

          function sampleTaskJSON() {
          /* eslint-disable camelcase */
            return {
              type:       "fetch",
              attributes: {
                pipeline: 'Build',
                stage:    "Dist",
                job:      "RPM",
                source:   'dir',
                is_source_a_file: true,
                destination: "Dest",
                run_if:   ['any'],
                on_cancel: {
                  type:       "nant",
                  attributes: {
                    build_file:        'build-moduleA.xml',
                    target:            'clean',
                    working_directory: "moduleA",
                    nant_path:         "C:\\NAnt",
                    run_if:            ['passed'],
                    on_cancel:         null
                  }
                }
              }
            };
          /* eslint-enable camelcase */
          }
        });
      });

      describe("Plugin Task", function () {
        beforeAll(function () {
          task = new Tasks.Task.PluginTask({
            pluginId:      'indix.s3fetch',
            version:       1,
            configuration: Tasks.Task.PluginTask.Configurations.fromJSON([
            {key: "Repo",    value: "foo"},
            {key: "Package", value: "foobar-widgets"}
            ]),
            runIf:        ['any']
          });
        });

        it("should initialize task model with type", function () {
          expect(task.type()).toBe("pluggable_task");
        });

        it("should initialize task model with pluginId", function () {
          expect(task.pluginId()).toBe("indix.s3fetch");
        });

        it("should initialize task model with version", function () {
          expect(task.version()).toBe(1);
        });

        it("should initialize task model with configuration", function () {
          expect(task.configuration().collectConfigurationProperty('key')).toEqual(['Repo', 'Package']);
          expect(task.configuration().collectConfigurationProperty('value')).toEqual(['foo', 'foobar-widgets']);
        });

        it("should initialize task model with runIfConditions", function () {
          expect(task.runIf().data()).toEqual(['any']);
        });

        it('should have a string representation', function () {
          expect(task.toString()).toBe('Repo: foo Package: foobar-widgets');
        });

        describe('from pluginInfo', function() {
          it('should be created from a plugin', function() {
            var plugin = new PluginInfos.PluginInfo({
              id:      'plugin_id',
              version: 'plugin_version'
            });

            var task = Tasks.Task.PluginTask.fromPluginInfo(plugin);

            expect(task.pluginId()).toBe('plugin_id');
            expect(task.version()).toBe('plugin_version');
            expect(task.configuration().isEmptyConfiguration()).toBe(true);
          });
        });

        describe("Serialize from/to JSON", function () {
          beforeEach(function () {
            var data = sampleTaskJSON();
            task     = Tasks.Task.fromJSON(data);
          });

          it("should de-serialize from json", function () {
            expect(task.type()).toBe("pluggable_task");
            expect(task.pluginId()).toBe("indix.s3fetch");
            expect(task.version()).toBe(1);
            expect(task.runIf().data()).toEqual(['any']);

            expect(task.configuration().collectConfigurationProperty('key')).toEqual(['Repo', 'Package']);
            expect(task.configuration().collectConfigurationProperty('value')).toEqual(['foo', 'foobar-widgets']);
          });

          it("should serialize to JSON", function () {
            expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
          });

          function sampleTaskJSON() {
          /* eslint-disable camelcase */
            return {
              type:       "pluggable_task",
              attributes: {
                plugin_configuration: {
                  id:     "indix.s3fetch",
                  version:       1
                },
                configuration: [
                {key: "Repo",    value: "foo"},
                {key: "Package", value: "foobar-widgets"}
                ],
                run_if:        ['any'],
                on_cancel: {
                  type:       "nant",
                  attributes: {
                    build_file:        'build-moduleA.xml',
                    target:            'clean',
                    working_directory: "moduleA",
                    nant_path:         "C:\\NAnt",
                    run_if:            ['passed'],
                    on_cancel:         null
                  }
                }
              }
            };
          /* eslint-enable camelcase */
          }
        });
      });
    });
  });
