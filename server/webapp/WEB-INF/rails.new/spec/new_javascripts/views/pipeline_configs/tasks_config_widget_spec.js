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

define(["jquery", "mithril", "lodash", "models/pipeline_configs/tasks", "views/pipeline_configs/tasks_config_widget"], function ($, m, _, Tasks, TasksConfigWidget) {
  describe("Tasks Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    describe('Ant Task View', function () {
      var task;
      beforeAll(function () {
        var tasks = m.prop(new Tasks());

        task = new Tasks.Task.Ant({
          /* eslint-disable camelcase */
          buildFile:        'build-moduleA.xml',
          target:           'clean',
          workingDirectory: 'moduleA',
          runIf:            ['passed', 'failed'],
          onCancelTask: {
            type:                "nant",
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

      it('should bind target', function () {
        expect($root.find("input[data-prop-name='target']").val()).toBe(task.target());
      });

      it('should bind build file', function () {
        expect($root.find("input[data-prop-name='buildFile']").val()).toBe(task.buildFile());
      });

      it('should bind working directory', function () {
        expect($root.find("input[data-prop-name='workingDirectory']").val()).toBe(task.workingDirectory());
      });

      it('should render run_if conditions', function () {
        expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
      });

      it('should render onCancel task', function () {
        expect($root.find("input[data-prop-name='target']").val()).toBe(task.onCancelTask.target());
        expect($root.find("input[data-prop-name='buildFile']").val()).toBe(task.onCancelTask.buildFile());
        expect($root.find("input[data-prop-name='workingDirectory']").val()).toBe(task.onCancelTask.workingDirectory());
        expect($root.find("input[data-prop-name='nantPath']").val()).toBe(task.onCancelTask.nantPath());
        expect($root.find("input[type=checkbox][data-prop-name=checked]").is(':checked')).toBe(true);
        expect($root.find("select :checked").val()).toBe(task.onCancelTask.type());
      });
    });

    describe('Nant Task View', function () {
      var task;
      beforeAll(function () {
        var tasks = m.prop(new Tasks());

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

      it('should bind target', function () {
        expect($root.find("input[data-prop-name='target']").val()).toBe(task.target());
      });

      it('should bind working directory', function () {
        expect($root.find("input[data-prop-name='buildFile']").val()).toBe(task.buildFile());
      });

      it('should bind build file', function () {
        expect($root.find("input[data-prop-name='workingDirectory']").val()).toBe(task.workingDirectory());
      });

      it('should bind nant path', function () {
        expect($root.find("input[data-prop-name='nantPath']").val()).toBe(task.nantPath());
      });

      it('should render run_if conditions', function () {
        expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
      });

      it('should not have onCancel task', function () {
        expect($root.find("input[type=checkbox][data-prop-name=checked]").is(':checked')).toBe(false);
      });
    });

    describe('Exec Task View', function () {
      var task;
      beforeAll(function () {
        var tasks = m.prop(new Tasks());

        task = new Tasks.Task.Exec({
          command:          'bash',
          args:             ['-c', 'ls -al /'],
          workingDirectory: 'moduleA',
          runIf:            ['passed', 'failed']
        });
        tasks().addTask(task);

        mount(tasks);
      });

      describe('render', function () {
        it('should bind the command', function () {
          expect($root.find("input[data-prop-name='command']").val()).toBe(task.command());
        });

        it('should bind the working directory', function () {
          expect($root.find("input[data-prop-name='workingDirectory']").val()).toBe(task.workingDirectory());
        });

        it('should bind the args', function () {
          expect($root.find("textarea[data-prop-name='data']").val()).toBe(task.args().data().join('\n'));
        });

        it('should render run_if conditions', function () {
          expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
          expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
          expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
        });

        it('should not have onCancel task', function () {
          expect($root.find("input[type=checkbox][data-prop-name=checked]").is(':checked')).toBe(false);
        });
      });
    });

    describe('Rake Task View', function () {
      var task;
      beforeAll(function () {
        var tasks = m.prop(new Tasks());

        task = new Tasks.Task.Rake({
          buildFile:        'foo.rake',
          target:           "clean",
          workingDirectory: "moduleA",
          runIf:            ['passed', 'failed']
        });
        tasks().addTask(task);

        mount(tasks);
      });

      it('should bind target', function () {
        expect($root.find("input[data-prop-name='target']").val()).toBe(task.target());
      });

      it('should bind working directory', function () {
        expect($root.find("input[data-prop-name='buildFile']").val()).toBe(task.buildFile());
      });

      it('should bind build file', function () {
        expect($root.find("input[data-prop-name='workingDirectory']").val()).toBe(task.workingDirectory());
      });

      it('should render run_if conditions', function () {
        expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
      });

      it('should not have onCancel task', function () {
        expect($root.find("input[type=checkbox][data-prop-name=checked]").is(':checked')).toBe(false);
      });
    });

    describe('FetchArtifact Task View', function () {
      var task;
      beforeAll(function () {
        var tasks = m.prop(new Tasks());

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

      it('should bind pipeline', function () {
        expect($root.find("input[data-prop-name='pipeline']").val()).toBe(task.pipeline());
      });

      it('should bind stage', function () {
        expect($root.find("input[data-prop-name='stage']").val()).toBe(task.stage());
      });

      it('should bind job', function () {
        expect($root.find("input[data-prop-name='job']").val()).toBe(task.job());
      });

      it('should bind source', function () {
        expect($root.find("input[data-prop-name='source']").val()).toBe(task.source());
      });

      it('should bind destination', function () {
        expect($root.find("input[data-prop-name='destination']").val()).toBe(task.destination());
      });

      it('should bind source is a file ', function () {
        expect($root.find("input[type=checkbox][data-prop-name=isSourceAFile]").is(':checked')).toBe(task.isSourceAFile());
      });

      it('should render run_if conditions', function () {
        expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
      });

      it('should not have onCancel task', function () {
        expect($root.find("input[type=checkbox][data-prop-name=checked]").is(':checked')).toBe(false);
      });
    });

    describe("Add Tasks", function () {
      var antTask, nantTask, execTask, rakeTask, fetchArtifactTask, tasks;
      beforeEach(function () {
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

        tasks = m.prop(new Tasks());

        _.each([
          antTask,
          nantTask,
          execTask,
          rakeTask,
          fetchArtifactTask
        ], function (task) {
          tasks().addTask(task);
        });

        mount(tasks);
      });

      it("should add a new task", function () {
        expect(tasks().countTask()).toBe(5);
        expect($root.find('.task-definition').length).toBe(5);

        var addTaskButton = $root.find('.add-button').get(0);
        var evObj = document.createEvent('MouseEvents');
        evObj.initEvent('click', true, false);
        addTaskButton.onclick(evObj);
        m.redraw(true);

        expect(tasks().countTask()).toBe(6);
        expect($root.find('.task-definition').length).toBe(6);
      });
    });

    var mount = function (tasks) {
      m.mount(root,
        m.component(TasksConfigWidget, {tasks: tasks})
      );
      m.redraw(true);
    };

  });
});
