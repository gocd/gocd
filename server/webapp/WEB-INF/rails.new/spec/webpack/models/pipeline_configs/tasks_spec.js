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
describe("Task Model", () => {

  const s = require("string-plus");

  const Tasks = require("models/pipeline_configs/tasks");

  const PluginInfos = require('models/shared/plugin_infos');

  let task;
  describe("Ant", () => {
    beforeEach(() => {
      task = new Tasks.Task.Ant({
        buildFile:        'build-moduleA.xml',
        target:           "clean",
        workingDirectory: "moduleA",
        runIf:            ['any']
      });
    });

    it("should initialize task model with type", () => {
      expect(task.type()).toBe("ant");
    });

    it("should initialize task model with target", () => {
      expect(task.target()).toBe("clean");
    });

    it("should initialize task model with workingDirectory", () => {
      expect(task.workingDirectory()).toBe("moduleA");
    });

    it("should initialize task model with runIfConditions", () => {
      expect(task.runIf().data()).toEqual(['any']);
    });

    it("should initialize task model with buildFile", () => {
      expect(task.buildFile()).toBe("build-moduleA.xml");
    });

    it('should have a string representation', () => {
      expect(task.toString()).toBe('clean build-moduleA.xml');
    });

    it('should initialize onCancel task', () => {
      const cancelTask = 'cancelTask';
      spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

      const task = new Tasks.Task.Ant({
        onCancelTask: {type: 'Nant'}
      });

      expect(task.onCancelTask).toBe(cancelTask);
    });

    it('should not have onCancel task if not specified', () => {
      const task = new Tasks.Task.Ant({
        onCancelTask: null
      });

      expect(task.onCancelTask).toBe(null);
    });

    describe("Serialization from/to JSON", () => {
      beforeEach(() => {
        task = Tasks.Task.fromJSON(sampleTaskJSON());
      });

      it("should de-serialize from JSON", () => {
        expect(task.type()).toBe("ant");
        expect(task.target()).toBe('clean');
        expect(task.workingDirectory()).toBe("moduleA");
        expect(task.runIf().data()).toEqual(['any']);
        expect(task.onCancelTask.type()).toBe('nant');
      });

      it("should serialize to JSON", () => {
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
            on_cancel:         {
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

  describe("NAnt", () => {
    beforeEach(() => {
      task = new Tasks.Task.NAnt({
        buildFile:        'build-moduleA.xml',
        target:           "clean",
        workingDirectory: "moduleA",
        nantPath:         'C:\\NAnt',
        runIf:            ['any']
      });
    });

    it("should initialize task model with type", () => {
      expect(task.type()).toBe("nant");
    });

    it("should initialize task model with target", () => {
      expect(task.target()).toBe("clean");
    });

    it("should initialize task model with workingDirectory", () => {
      expect(task.workingDirectory()).toBe("moduleA");
    });

    it("should initialize task model with buildFile", () => {
      expect(task.buildFile()).toBe("build-moduleA.xml");
    });
    it("should initialize task model with nantPath", () => {
      expect(task.nantPath()).toBe("C:\\NAnt");
    });

    it("should initialize task model with runIfConditions", () => {
      expect(task.runIf().data()).toEqual(['any']);
    });

    it('should have a string representation', () => {
      expect(task.toString()).toBe('clean build-moduleA.xml');
    });

    it('should initialize onCancel task', () => {
      const cancelTask = 'cancelTask';
      spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

      const task = new Tasks.Task.NAnt({
        onCancelTask: {type: 'Nant'}
      });

      expect(task.onCancelTask).toBe(cancelTask);
    });

    it('should not have onCancel task if not specified', () => {
      const task = new Tasks.Task.NAnt({
        onCancelTask: null
      });

      expect(task.onCancelTask).toBe(null);
    });
    describe("Serialize from/to JSON", () => {
      beforeEach(() => {
        task = Tasks.Task.fromJSON(sampleTaskJSON());
      });

      it("should de-serialize from JSON", () => {
        expect(task.type()).toBe("nant");
        expect(task.target()).toBe('clean');
        expect(task.workingDirectory()).toBe("moduleA");
        expect(task.nantPath()).toBe("C:\\NAnt");
        expect(task.runIf().data()).toEqual(['any']);
      });

      it("should serialize to JSON", () => {
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
            on_cancel:         {
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

  describe("Exec", () => {
    describe('initialize', () => {
      let taskJSON, task;
      beforeEach(() => {
        taskJSON = {
          command:          'bash',
          workingDirectory: 'moduleA',
          runIf:            ['any']
        };

        task = new Tasks.Task.Exec(taskJSON);
      });

      it("should initialize task model with command", () => {
        expect(task.type()).toBe("exec");
      });

      it("should initialize task model with workingDirectory", () => {
        expect(task.workingDirectory()).toBe("moduleA");
      });

      it("should initialize task model with args as list", () => {
        taskJSON['arguments'] = ['-c', 'ls -al /'];

        const task = new Tasks.Task.Exec(taskJSON);

        expect(task.args().data()).toEqual(['-c', 'ls -al /']);
      });

      it("should initialize task model with args as string", () => {
        taskJSON['args'] = '-a';

        const task = new Tasks.Task.Exec(taskJSON);

        expect(task.args().data()).toEqual('-a');
      });

      it("should initialize task model with runIfConditions", () => {
        expect(task.runIf().data()).toEqual(['any']);
      });

      it('should have a string representation', () => {
        taskJSON['args'] = '-a';
        const task         = new Tasks.Task.Exec(taskJSON);

        expect(task.toString()).toBe("bash -a");
      });

      it('should initialize onCancel task', () => {
        const cancelTask = 'cancelTask';
        spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

        const task = new Tasks.Task.Exec({
          onCancelTask: {type: 'Nant'}
        });

        expect(task.onCancelTask).toBe(cancelTask);
      });

      it('should not have onCancel task if not specified', () => {
        const task = new Tasks.Task.Exec({
          onCancelTask: null
        });

        expect(task.onCancelTask).toBe(null);
      });
    });

    describe('validation', () => {
      it('should validate presence of command', () => {
        const task = new Tasks.Task.Exec({});

        expect(task.isValid()).toBe(false);
        expect(task.errors().errors('command')).toEqual(['Command must be present']);
      });
    });

    describe("Serialize from/to JSON", () => {
      beforeEach(() => {
        task = Tasks.Task.fromJSON(sampleTaskJSON());
      });

      it("should de-serialize from JSON", () => {
        expect(task.type()).toBe("exec");
        expect(task.command()).toBe('bash');
        expect(task.args().data()).toEqual(['-c', 'ls -al /']);
        expect(task.runIf().data()).toEqual(['any']);
      });

      it('should map server side errors', () => {
        const task = Tasks.Task.fromJSON({
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

      it("should serialize to JSON", () => {
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
            on_cancel:         {
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

  describe("Rake", () => {
    beforeEach(() => {
      task = new Tasks.Task.Rake({
        buildFile:        'foo.rake',
        target:           "clean",
        workingDirectory: "moduleA",
        runIf:            ['any']
      });
    });

    it("should initialize task model with type", () => {
      expect(task.type()).toBe("rake");
    });

    it("should initialize task model with target", () => {
      expect(task.target()).toBe("clean");
    });

    it("should initialize task model with workingDirectory", () => {
      expect(task.workingDirectory()).toBe("moduleA");
    });

    it("should initialize task model with buildFile", () => {
      expect(task.buildFile()).toBe("foo.rake");
    });

    it("should initialize task model with runIfConditions", () => {
      expect(task.runIf().data()).toEqual(['any']);
    });

    it('should have a string representation', () => {
      expect(task.toString()).toBe('clean foo.rake');
    });

    it('should initialize onCancel task', () => {
      const cancelTask = 'cancelTask';
      spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

      const task = new Tasks.Task.Rake({
        onCancelTask: {type: 'Nant'}
      });

      expect(task.onCancelTask).toBe(cancelTask);
    });

    it('should not have onCancel task if not specified', () => {
      const task = new Tasks.Task.Rake({
        onCancelTask: null
      });

      expect(task.onCancelTask).toBe(null);
    });
    describe("Serialize from/to JSON", () => {
      beforeEach(() => {
        task = Tasks.Task.fromJSON(sampleTaskJSON());
      });

      it("should de-serialize from json", () => {
        expect(task.type()).toBe("rake");
        expect(task.target()).toBe('clean');
        expect(task.workingDirectory()).toBe("moduleA");
        expect(task.runIf().data()).toEqual(['any']);
      });

      it("should serialize to JSON", () => {
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
            on_cancel:         {
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

  describe("FetchArtifact", () => {
    beforeEach(() => {
      task = new Tasks.Task.FetchArtifact({
        pipeline:      'Build',
        stage:         "Dist",
        job:           "RPM",
        source:        "dir",
        isSourceAFile: true,
        destination:   "Dest",
        runIf:         ['any']
      });
    });

    it("should initialize task model with pipeline", () => {
      expect(task.pipeline()).toBe("Build");
    });

    it("should initialize task model with stage", () => {
      expect(task.stage()).toBe("Dist");
    });

    it("should initialize task model with job", () => {
      expect(task.job()).toBe("RPM");
    });

    it("should initialize task model with source", () => {
      expect(task.source()).toBe("dir");
    });

    it("should initialize task model with isSourceAFile", () => {
      expect(task.isSourceAFile()).toBe(true);
    });

    it("should initialize task model with Destination", () => {
      expect(task.destination()).toBe("Dest");
    });

    it('should initialize isSourceAFile to be false if not specified', () => {
      const task = new Tasks.Task.FetchArtifact({});
      expect(task.isSourceAFile()).toBe(false);
    });

    it("should initialize task model with runIfConditions", () => {
      expect(task.runIf().data()).toEqual(['any']);
    });

    it('should have a string representation', () => {
      expect(task.toString()).toBe('Build Dist RPM');
    });

    it('should initialize onCancel task', () => {
      const cancelTask = 'cancelTask';
      spyOn(Tasks.Task, 'fromJSON').and.returnValue(cancelTask);

      const task = new Tasks.Task.FetchArtifact({
        onCancelTask: {type: 'Nant'}
      });

      expect(task.onCancelTask).toBe(cancelTask);
    });

    it('should not have onCancel task if not specified', () => {
      const task = new Tasks.Task.FetchArtifact({
        onCancelTask: null
      });

      expect(task.onCancelTask).toBe(null);
    });

    it('should validate presence of required fields', () => {
      const task = new Tasks.Task.FetchArtifact({});

      expect(task.isValid()).toBe(false);
      expect(task.errors().errors('stage')).toEqual(['Stage must be present']);
      expect(task.errors().errors('job')).toEqual(['Job must be present']);
      expect(task.errors().errors('source')).toEqual(['Source must be present']);
    });

    describe("Serialize from/to JSON", () => {
      beforeEach(() => {
        task = Tasks.Task.fromJSON(sampleTaskJSON());
      });

      it("should de-serialize from JSON", () => {
        expect(task.type()).toBe("fetch");
        expect(task.pipeline()).toBe("Build");
        expect(task.stage()).toBe("Dist");
        expect(task.job()).toBe("RPM");
        expect(task.source()).toBe("dir");
        expect(task.isSourceAFile()).toBe(true);
        expect(task.destination()).toBe("Dest");
        expect(task.runIf().data()).toEqual(['any']);
      });

      it('should map server side errors', () => {
        const task = Tasks.Task.fromJSON({
          type:   "fetch",
          errors: {
            job:    [
              'Job is a required field.'
            ],
            source: [
              'Should provide either srcdir or srcfile'
            ],
            stage:  [
              'Stage is a required field.'
            ]
          }
        });

        expect(task.errors()._isEmpty()).toBe(false);
        expect(task.errors().errors('stage')).toEqual(['Stage is a required field.']);
        expect(task.errors().errors('job')).toEqual(['Job is a required field.']);
        expect(task.errors().errors('source')).toEqual(['Should provide either srcdir or srcfile']);
      });

      it("should serialize to JSON", () => {
        expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
      });

      function sampleTaskJSON() {
        /* eslint-disable camelcase */
        return {
          type:       "fetch",
          attributes: {
            pipeline:         'Build',
            stage:            "Dist",
            job:              "RPM",
            source:           'dir',
            is_source_a_file: true,
            destination:      "Dest",
            run_if:           ['any'],
            on_cancel:        {
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

  describe("Plugin Task", () => {
    beforeEach(() => {
      task = new Tasks.Task.PluginTask({
        pluginId:      'indix.s3fetch',
        version:       1,
        configuration: Tasks.Task.PluginTask.Configurations.fromJSON([
          {key: "Repo", value: "foo"},
          {key: "Package", value: "foobar-widgets"}
        ]),
        runIf:         ['any']
      });
    });

    it("should initialize task model with type", () => {
      expect(task.type()).toBe("pluggable_task");
    });

    it("should initialize task model with pluginId", () => {
      expect(task.pluginId()).toBe("indix.s3fetch");
    });

    it("should initialize task model with version", () => {
      expect(task.version()).toBe(1);
    });

    it("should initialize task model with configuration", () => {
      expect(task.configuration().collectConfigurationProperty('key')).toEqual(['Repo', 'Package']);
      expect(task.configuration().collectConfigurationProperty('value')).toEqual(['foo', 'foobar-widgets']);
    });

    it("should initialize task model with runIfConditions", () => {
      expect(task.runIf().data()).toEqual(['any']);
    });

    it('should have a string representation', () => {
      expect(task.toString()).toBe('Repo: foo Package: foobar-widgets');
    });

    describe('from pluginInfo', () => {
      it('should be created from a plugin', () => {
        const json = {
          "id":            "script-executor",
          "type":          "task",
          "status":        {
            "state": "active"
          },
          "about":         {
            "name":                     "Script Executor",
            "version":                  "0.1.27",
            "target_go_version":        "14.4.0",
            "description":              "Script executor",
            "target_operating_systems": [
              "Linux",
              "Mac OS X"
            ],
            "vendor":                   {
              "name": "foo",
              "url":  "http://foo"
            }
          },
          "extension_info": {
            "display_name":  "Script Executor",
            "task_settings": {
              "configurations": [
                {
                  "key":      "script",
                  "metadata": {
                    "secure":   false,
                    "required": true
                  }
                }
              ],
              "view":           {
                "template": "Script executor task view"
              }
            }
          }
        };

        const plugin = PluginInfos.PluginInfo.fromJSON(json);
        const task = Tasks.Task.PluginTask.fromPluginInfo(plugin);

        expect(task.pluginId()).toBe('script-executor');
        expect(task.version()).toBe('0.1.27');
      });
    });

    describe("Serialize from/to JSON", () => {
      beforeEach(() => {
        const data = sampleTaskJSON();
        task     = Tasks.Task.fromJSON(data);
      });

      it("should de-serialize from json", () => {
        expect(task.type()).toBe("pluggable_task");
        expect(task.pluginId()).toBe("indix.s3fetch");
        expect(task.version()).toBe(1);
        expect(task.runIf().data()).toEqual(['any']);

        expect(task.configuration().collectConfigurationProperty('key')).toEqual(['Repo', 'Package']);
        expect(task.configuration().collectConfigurationProperty('value')).toEqual(['foo', 'foobar-widgets']);
      });

      it("should serialize to JSON", () => {
        expect(JSON.parse(JSON.stringify(task, s.snakeCaser))).toEqual(sampleTaskJSON());
      });

      function sampleTaskJSON() {
        /* eslint-disable camelcase */
        return {
          type:       "pluggable_task",
          attributes: {
            plugin_configuration: {
              id:      "indix.s3fetch",
              version: 1
            },
            configuration:        [
              {key: "Repo", value: "foo"},
              {key: "Package", value: "foobar-widgets"}
            ],
            run_if:               ['any'],
            on_cancel:            {
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
