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

describe("TasksConfigWidget", () => {

  const m             = require('mithril');
  const Stream        = require('mithril/stream');
  const _             = require("lodash");
  const simulateEvent = require('simulate-event');

  require('jasmine-jquery');

  const Tasks             = require("models/pipeline_configs/tasks");
  const PluginInfos       = require("models/shared/plugin_infos");
  const TasksConfigWidget = require("views/pipeline_configs/tasks_config_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  describe('Ant Task View', () => {
    let task;
    beforeEach(() => {
      const tasks = Stream(new Tasks());

      task = new Tasks.Task.Ant({
        /* eslint-disable camelcase */
        buildFile:        'build-moduleA.xml',
        target:           'clean',
        workingDirectory: 'moduleA',
        runIf:            ['passed', 'failed'],
        onCancelTask:     {
          type:       "nant",
          attributes: {
            build_file:        'build-moduleA.xml',
            target:            'clean',
            working_directory: "moduleA",
            nant_path:         "C:\\NAnt",
            run_if:            []
          }
        }
        /* eslint-enable camelcase */
      });
      tasks().addTask(task);

      mount(tasks);
    });

    afterEach(() => {
      unmount();
    });

    it('should bind target', () => {
      expect($root.find("input[data-prop-name='target']")).toHaveValue(task.target());
    });

    it('should bind build file', () => {
      expect($root.find("input[data-prop-name='buildFile']")).toHaveValue(task.buildFile());
    });

    it('should bind working directory', () => {
      expect($root.find("input[data-prop-name='workingDirectory']")).toHaveValue(task.workingDirectory());
    });

    it('should render run_if conditions', () => {
      expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
    });

    it('should render onCancel task', () => {
      expect($root.find("input[data-prop-name='target']")).toHaveValue(task.onCancelTask.target());
      expect($root.find("input[data-prop-name='buildFile']")).toHaveValue(task.onCancelTask.buildFile());
      expect($root.find("input[data-prop-name='workingDirectory']")).toHaveValue(task.onCancelTask.workingDirectory());
      expect($root.find("input[data-prop-name='nantPath']")).toHaveValue(task.onCancelTask.nantPath());
      expect($root.find("input[type=checkbox][data-prop-name=checked]")).toBeChecked();
      expect($root.find('.on-cancel>div>.end>select :checked')).toHaveValue(task.onCancelTask.type());
    });
  });

  describe('Nant Task View', () => {
    let task;
    beforeEach(() => {
      const tasks = Stream(new Tasks());

      task = new Tasks.Task.NAnt({
        buildFile:        'build-moduleA.xml',
        target:           "clean",
        workingDirectory: "moduleA",
        nantPath:         'C:\\NAnt',
        runIf:            ['passed', 'failed']
      });
      tasks().addTask(task);

      mount(tasks);
    });

    afterEach(() => {
      unmount();
    });

    it('should bind target', () => {
      expect($root.find("input[data-prop-name='target']")).toHaveValue(task.target());
    });

    it('should bind working directory', () => {
      expect($root.find("input[data-prop-name='buildFile']")).toHaveValue(task.buildFile());
    });

    it('should bind build file', () => {
      expect($root.find("input[data-prop-name='workingDirectory']")).toHaveValue(task.workingDirectory());
    });

    it('should bind nant path', () => {
      expect($root.find("input[data-prop-name='nantPath']")).toHaveValue(task.nantPath());
    });

    it('should render run_if conditions', () => {
      expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
    });

    it('should not have onCancel task', () => {
      expect($root.find("input[type=checkbox][data-prop-name=checked]")).not.toBeChecked();
    });
  });

  describe('Exec Task View', () => {
    let task;
    beforeEach(() => {
      const tasks = Stream(new Tasks());

      task = new Tasks.Task.Exec({
        command:          'bash',
        args:             ['-c', 'ls -al /'],
        workingDirectory: 'moduleA',
        runIf:            ['passed', 'failed']
      });
      tasks().addTask(task);

      mount(tasks);
    });

    afterEach(() => {
      unmount();
    });

    describe('render', () => {
      it('should bind the command', () => {
        expect($root.find("input[data-prop-name='command']")).toHaveValue(task.command());
      });

      it('should bind the working directory', () => {
        expect($root.find("input[data-prop-name='workingDirectory']")).toHaveValue(task.workingDirectory());
      });

      it('should bind the args', () => {
        expect($root.find("textarea[data-prop-name='data']")).toHaveValue(task.args().data().join('\n'));
      });

      it('should render run_if conditions', () => {
        expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
      });

      it('should not have onCancel task', () => {
        expect($root.find("input[type=checkbox][data-prop-name=checked]")).not.toBeChecked();
      });
    });
  });

  describe('Rake Task View', () => {
    let task;
    beforeEach(() => {
      const tasks = Stream(new Tasks());

      task = new Tasks.Task.Rake({
        buildFile:        'foo.rake',
        target:           "clean",
        workingDirectory: "moduleA",
        runIf:            ['passed', 'failed']
      });
      tasks().addTask(task);

      mount(tasks);
    });

    afterEach(() => {
      unmount();
    });

    it('should bind target', () => {
      expect($root.find("input[data-prop-name='target']")).toHaveValue(task.target());
    });

    it('should bind working directory', () => {
      expect($root.find("input[data-prop-name='buildFile']")).toHaveValue(task.buildFile());
    });

    it('should bind build file', () => {
      expect($root.find("input[data-prop-name='workingDirectory']")).toHaveValue(task.workingDirectory());
    });

    it('should render run_if conditions', () => {
      expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
    });

    it('should not have onCancel task', () => {
      expect($root.find("input[type=checkbox][data-prop-name=checked]")).not.toBeChecked();
    });
  });

  describe('FetchArtifact Task View', () => {
    let task;
    beforeEach(() => {
      const tasks = Stream(new Tasks());

      task = new Tasks.Task.FetchArtifact({
        pipeline:      'Build',
        stage:         "Dist",
        job:           "RPM",
        source:        "dir",
        destination:   "Dest",
        isSourceAFile: true,
        runIf:         ['passed', 'failed']
      });
      tasks().addTask(task);

      mount(tasks);
    });

    afterEach(() => {
      unmount();
    });

    it('should bind pipeline', () => {
      expect($root.find("input[data-prop-name='pipeline']")).toHaveValue(task.pipeline());
    });

    it('should bind stage', () => {
      expect($root.find("input[data-prop-name='stage']")).toHaveValue(task.stage());
    });

    it('should bind job', () => {
      expect($root.find("input[data-prop-name='job']")).toHaveValue(task.job());
    });

    it('should bind source', () => {
      expect($root.find("input[data-prop-name='source']")).toHaveValue(task.source());
    });

    it('should bind destination', () => {
      expect($root.find("input[data-prop-name='destination']")).toHaveValue(task.destination());
    });

    it('should bind source is a file ', () => {
      expect($root.find("input[type=checkbox][data-prop-name=isSourceAFile]").is(':checked')).toBe(task.isSourceAFile());
    });

    it('should render run_if conditions', () => {
      expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
    });

    it('should not have onCancel task', () => {
      expect($root.find("input[type=checkbox][data-prop-name=checked]")).not.toBeChecked();
    });
  });

  describe('Plugin Task View', () => {
    describe('Missing Plugin', () => {
      let task;
      beforeEach(() => {
        const tasks = Stream(new Tasks());

        task = new Tasks.Task.PluginTask({
          pluginId:      'indix.s3fetch',
          version:       1,
          configuration: Tasks.Task.PluginTask.Configurations.fromJSON([
            {key: "Repo", value: "foo"},
            {key: "Package", value: "foobar-widgets"}
          ]),
          runIf:         ['any']
        });
        tasks().addTask(task);

        mount(tasks);
      });

      afterEach(() => {
        unmount();
      });

      afterAll(() => {
        unmount();
      });

      describe('render', () => {
        it('should show missing plugin error when no plugin is available', () => {
          expect($root.find(".pluggable-task>.alert")).toContainText("Plugin 'indix.s3fetch' not found.");
        });
      });
    });
  });

  describe("Add Tasks", () => {
    let antTask, nantTask, execTask, rakeTask, fetchArtifactTask, tasks;
    beforeEach(() => {
      antTask = new Tasks.Task.Ant({
        buildFile:        'build-moduleA.xml',
        target:           "clean",
        workingDirectory: "moduleA"
      });

      nantTask = new Tasks.Task.NAnt({
        buildFile:        'build-moduleA.xml',
        target:           "clean",
        workingDirectory: "moduleA",
        nantPath:         'C:\\NAnt'
      });

      execTask = new Tasks.Task.Exec({
        command:          'bash',
        args:             ['-c', 'ls -al /'],
        workingDirectory: "moduleA"
      });

      rakeTask = new Tasks.Task.Rake({
        buildFile:        'foo.rake',
        target:           "clean",
        workingDirectory: "moduleA"
      });

      fetchArtifactTask = new Tasks.Task.FetchArtifact({
        pipeline:     'Build',
        stage:        "Dist",
        job:          "RPM",
        source:       "dir",
        destination:  "Dest",
        sourceIsFile: true
      });

      tasks = Stream(new Tasks());

      _.each([
        antTask,
        nantTask,
        execTask,
        rakeTask,
        fetchArtifactTask
      ], (task) => {
        tasks().addTask(task);
      });

      mount(tasks);
    });

    afterEach(() => {
      unmount();
    });

    it("should add a new task", () => {
      expect(tasks().countTask()).toBe(5);
      expect($root.find('.task-definition')).toHaveLength(5);

      simulateEvent.simulate($root.find('.task-selector>div>.add-button').get(0), 'click');
      m.redraw();

      expect(tasks().countTask()).toBe(6);
      expect($root.find('.task-definition')).toHaveLength(6);
    });
  });

  const mount = (tasks) => {
    m.mount(root, {
      view() {
        return m(TasksConfigWidget, {
          tasks,
          pluginInfos: Stream(PluginInfos.fromJSON([taskPlugin]))
        });
      }
    });
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  const taskPlugin = {
    "id":            "script-executor",
    "version":       "1",
    "type":          "task",
    "status": {
      "state": "active"
    },
    "about":         {
      "name":                     "Script Executor",
      "version":                  "0.3.0",
      "target_go_version":        "16.1.0",
      "description":              "Thoughtworks Go plugin to run scripts",
      "target_operating_systems": [],
      "vendor":                   {
        "name": "Srinivas Upadhya",
        "url":  "https://github.com/srinivasupadhya"
      }
    },
    "display_name":  "Script Executor",
    "task_settings": {
      "configurations": [{"key": "script", "metadata": {"secure": false, "required": true}},
        {"key": "shtype", "metadata": {"secure": false, "required": true}}
      ],
      "view":           {"template": "<div />"}
    }
  };

});
