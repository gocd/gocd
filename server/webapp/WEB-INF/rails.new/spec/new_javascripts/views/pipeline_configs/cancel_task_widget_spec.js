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

define(["jquery", "mithril", "models/pipeline_configs/tasks", "views/pipeline_configs/cancel_task_widget"], function ($, m, Tasks, CancelTaskWidget) {
  describe('Cancel Task Widget', function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);
    var task;
    describe('view task with onCancel task', function () {
      beforeAll(function () {
        /* eslint-disable camelcase */
        task = new Tasks.Task.Ant({
          buildFile:        'build-moduleA.xml',
          target:           'clean',
          workingDirectory: 'moduleA',
          onCancelTask: {
            type: "nant",
            attributes: {
              build_file:        'build-moduleA.xml',
              target:            'clean',
              working_directory: "moduleA",
              nant_path:         "C:\\NAnt",
              run_if:            []
            }
          }
        });
        /* eslint-enable camelcase */

        mount(task);
      });

      afterAll(function () {
        unmount();
      });

      it('should bind target', function () {
        expect($root.find("input[data-prop-name='target']")).toHaveValue(task.onCancelTask.target());
      });

      it('should bind working directory', function () {
        expect($root.find("input[data-prop-name='buildFile']")).toHaveValue(task.onCancelTask.buildFile());
      });

      it('should bind build file', function () {
        expect($root.find("input[data-prop-name='workingDirectory']")).toHaveValue(task.onCancelTask.workingDirectory());
      });

      it('should bind nant path', function () {
        expect($root.find("input[data-prop-name='nantPath']")).toHaveValue(task.onCancelTask.nantPath());
      });

      it('should have the on cancel task checkbox checked', function () {
        expect($root.find("input[type=checkbox][data-prop-name=checked]")).toBeChecked();
      });

      it('should have onCancelTask type selected in dropdown', function () {
        expect($root.find("select :checked")).toHaveValue(task.onCancelTask.type());
      });
    });

    describe('view task without onCancel task', function () {
      it('should render only checkbox to enable onCancel task', function () {
        var task = new Tasks.Task.Ant({
          buildFile:        'build-moduleA.xml',
          target:           'clean',
          workingDirectory: 'moduleA',
          onCancelTask:     null
        });

        mount(task);

        expect($root.find("input[type=checkbox][data-prop-name=checked]")).not.toBeChecked();
        expect($root.find("input").size()).toBe(1);
        expect($root.find("select").size()).toBe(0);

        unmount();
      });
    });

    describe('view on selection of onCancel', function () {
      it('should render exec task on selection', function () {
        var task = new Tasks.Task.Ant({
          buildFile:        'build-moduleA.xml',
          target:           'clean',
          workingDirectory: 'moduleA',
          onCancelTask:      null
        });

        mount(task);

        $root.find("input[type=checkbox][data-prop-name=checked]").click();
        m.redraw(true);

        expect($root.find("select :checked")).toHaveValue('exec');
        expect(task.onCancelTask.type()).toBe('exec');

        unmount();
      });

    });

    describe('change onCancel task', function() {
      it('should allow selecting a different onCancel task', function() {
        /* eslint-disable camelcase */
        var task = new Tasks.Task.Ant({
          buildFile:        'build-moduleA.xml',
          target:           'clean',
          workingDirectory: 'moduleA',
          onCancelTask: {
            type: "nant",
            attributes: {
              build_file:        'build-moduleA.xml',
              target:            'clean',
              working_directory: "moduleA",
              nant_path:         "C:\\NAnt",
              run_if:            []
            }
          }
        });
        /* eslint-enable camelcase */

        mount(task);

        $root.find('.on-cancel select').val('exec');
        $root.find('.on-cancel select').change();
        m.redraw(true);

        expect(task.onCancelTask.type()).toBe('exec');
        expect($root.find("input[data-prop-name='command']")).toHaveValue(task.onCancelTask.command());

        unmount();
      });

    });

    var unmount = function () {
      m.mount(root, null);
      m.redraw(true);
    };

    var mount = function (task) {
      m.mount(root,
        m.component(CancelTaskWidget, {task: task})
      );
      m.redraw(true);
    };

  });
});
