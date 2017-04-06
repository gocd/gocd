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

describe("Read Only Tasks Config Widget", () => {
  const $             = require("jquery");
  const m             = require("mithril");
  const Stream        = require("mithril/stream");
  const PluginInfos   = require('models/shared/plugin_infos');
  const simulateEvent = require('simulate-event');
  require('jasmine-jquery');

  const TasksConfigWidget = require("views/pipeline_configs/read_only/tasks_config_widget");
  const Jobs              = require("models/pipeline_configs/jobs");

  let root, jobs;
  beforeEach(() => {
    [, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  describe('Exec Task', () => {
    beforeEach(() => {
      rawJobsJSON[0].tasks = [
        {
          "type":       "exec",
          "attributes": {
            "run_if":            [
              "passed"
            ],
            "on_cancel":         null,
            "command":           "ls",
            "working_directory": null
          }
        }
      ];

      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render Tasks heading', () => {
      expect($('h5')).toContainText('Tasks:');
    });

    it('should render the tasks heading in tabular format', () => {
      const headings = $.find('table>tr>th');
      expect(headings[0]).toContainText('Task Type');
      expect(headings[1]).toContainText('Run If');
      expect(headings[2]).toContainText('Properties');
      expect(headings[3]).toContainText('On Cancel');
    });

    it('should render the task', () => {
      const row1 = $($.find('table>tr')[1]).children();
      expect($(row1[0])).toContainText('exec');
      expect($(row1[1])).toContainText('passed');
      expect($(row1[2])).toContainText('command: ls');
      expect($(row1[3])).toContainText('-');
    });
  });

  describe('Pluggable Task', () => {
    beforeEach(() => {
      rawJobsJSON[0].tasks = [
        {
          "type":       "pluggable_task",
          "attributes": {
            "run_if":               ["failed"],
            "on_cancel":            null,
            "plugin_configuration": {
              "id":      "script-executor",
              "version": "1"
            },
            "configuration":        [
              {
                "key":   "script",
                "value": "foobar"
              },
              {
                "key":   "shtype",
                "value": "bash"
              }
            ]
          }
        }
      ];

      const pluginInfosJSON = [{
        "id":            "script-executor",
        "type":          "task",
        "about":         {
          "name":    "Script Executor",
          "version": "1",
        },
        "display_name":  "Script Executor",
        "task_settings": {
          "configurations": [],
          "view":           {"template": "<div/>"}
        }
      }];

      jobs = Jobs.fromJSON(rawJobsJSON);
      mount(pluginInfosJSON);
    });

    afterEach(() => {
      unmount();
    });

    it('should render the plugabble task', () => {
      const row1 = $($.find('table>tr')[1]).children();
      expect($(row1[0])).toContainText('Script Executor');
      expect($(row1[1])).toContainText('failed');
      expect($(row1[2])).toContainText('Script: foobar');
      expect($(row1[2])).toContainText('Shtype: bash');
      expect($(row1[3])).toContainText('-');
    });
  });

  describe('OnCancel', () => {
    beforeEach(() => {
      rawJobsJSON[0].tasks = [
        {
          "type":       "exec",
          "attributes": {
            "run_if":            [
              "passed"
            ],
            "on_cancel":         {
              "type":       "exec",
              "attributes": {
                "run_if":            [],
                "on_cancel":         null,
                "command":           "ls",
                "arguments":         [
                  "something"
                ],
                "working_directory": null
              }
            },
            "command":           "ls",
            "working_directory": null
          }
        }
      ];

      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render the oncancel task', () => {
      const row1 = $($.find('table>tr')[1]).children();
      expect($(row1[3])).toContainText('exec');

      expect('.oncancel-task-description').toContainText('command: ls');
      expect('.oncancel-task-description').toContainText('args: something');
    });

    it('should toggle the oncancel task view', () => {
      expect('.oncancel-task-description').not.toHaveClass('is-open');

      simulateEvent.simulate($('.has-oncancel-task-drop-down').get(0), 'click');
      expect('.oncancel-task-description').toHaveClass('is-open');

      simulateEvent.simulate($('.has-oncancel-task-drop-down').get(0), 'click');
      expect('.oncancel-task-description').not.toHaveClass('is-open');
    });
  });

  const mount = function (pluginInfosJSON = []) {
    m.mount(root, {
      view () {
        return m(TasksConfigWidget, {
          tasks:       jobs.firstJob().tasks,
          pluginInfos: Stream(PluginInfos.fromJSON(pluginInfosJSON))
        });
      }
    });

    m.redraw();
  };

  const unmount = function () {
    m.mount(root, null);
    m.redraw();
  };

  const rawJobsJSON = [
    {
      "name":  "up42_job",
      "tasks": []
    }
  ];
});
