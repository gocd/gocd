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

describe('Cancel Task Widget', () => {

  const $             = require("jquery");
  const m             = require("mithril");
  const Stream        = require("mithril/stream");
  const simulateEvent = require('simulate-event');
  require('jasmine-jquery');

  const Tasks            = require("models/pipeline_configs/tasks");
  const PluginInfos      = require("models/shared/plugin_infos");
  const CancelTaskWidget = require("views/pipeline_configs/cancel_task_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);
  let task;
  describe('view task with onCancel task', () => {
    beforeEach(() => {
      /* eslint-disable camelcase */
      task = new Tasks.Task.Ant({
        buildFile:        'build-moduleA.xml',
        target:           'clean',
        workingDirectory: 'moduleA',
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
      });
      /* eslint-enable camelcase */

      mount(task);
    });

    afterEach(() => {
      unmount();
    });

    it('should bind target', () => {
      expect($root.find("input[data-prop-name='target']")).toHaveValue(task.onCancelTask.target());
    });

    it('should bind working directory', () => {
      expect($root.find("input[data-prop-name='buildFile']")).toHaveValue(task.onCancelTask.buildFile());
    });

    it('should bind build file', () => {
      expect($root.find("input[data-prop-name='workingDirectory']")).toHaveValue(task.onCancelTask.workingDirectory());
    });

    it('should bind nant path', () => {
      expect($root.find("input[data-prop-name='nantPath']")).toHaveValue(task.onCancelTask.nantPath());
    });

    it('should have the on cancel task checkbox checked', () => {
      expect($root.find("input[type=checkbox][data-prop-name=checked]")).toBeChecked();
    });

    it('should have onCancelTask type selected in dropdown', () => {
      expect($root.find("select :checked")).toHaveValue(task.onCancelTask.type());
    });
  });

  describe('view task without onCancel task', () => {
    it('should render only checkbox to enable onCancel task', () => {
      const task = new Tasks.Task.Ant({
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

  describe('view on selection of onCancel', () => {
    it('should render exec task on selection', () => {
      //todo: fix asnyc redraw

      const task = new Tasks.Task.Ant({
        buildFile:        'build-moduleA.xml',
        target:           'clean',
        workingDirectory: 'moduleA',
        onCancelTask:     null
      });

      mount(task);

      $root.find("input[type=checkbox][data-prop-name=checked]").click();
      m.redraw();

      expect($root.find("select :checked")).toHaveValue('exec');
      expect(task.onCancelTask.type()).toBe('exec');

      unmount();
    });

  });

  describe('change onCancel task', () => {
    it('should allow selecting a different onCancel task', () => {

      /* eslint-disable camelcase */
      const task = new Tasks.Task.Ant({
        buildFile:        'build-moduleA.xml',
        target:           'clean',
        workingDirectory: 'moduleA',
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
      });
      /* eslint-enable camelcase */

      mount(task);

      const dropDown = $root.find('.on-cancel select').get(0);
      $(dropDown).val('exec');
      simulateEvent.simulate(dropDown, 'change');

      m.redraw();

      expect(task.onCancelTask.type()).toBe('exec');
      expect($root.find("input[data-prop-name='command']")).toHaveValue(task.onCancelTask.command());

      unmount();
    });

  });

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  const mount = (task) => {
    m.mount(root, {
      view() {
        return m(CancelTaskWidget, {
          task,
          pluginInfos: Stream(PluginInfos.fromJSON([]))
        });
      }
    });
    m.redraw();
  };

});
