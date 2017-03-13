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

describe("RunIfConditions Widget", () => {

  var m = require("mithril");

  var RunIfConditionsWidget = require("views/pipeline_configs/run_if_conditions_widget");
  var Tasks                 = require("models/pipeline_configs/tasks");

  var $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  function mount(task) {
    m.mount(root, {
      view() {
        return m(RunIfConditionsWidget, {task});
      }
    });
    m.redraw(true);
  }

  var unmount = () => {
    m.mount(root, null);
    m.redraw(true);
  };

  describe("View", () => {
    afterEach(() => {
      unmount();
    });

    it("should render checkbox for runIf conditions", () => {
      var task = new Tasks.Task.Exec({runIf: ['any']});
      mount(task);

      expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
      expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
    });

    it("should have run_if conditions checked for tasks with runIf", () => {
      var task = new Tasks.Task.Exec({runIf: ['passed', 'failed']});
      mount(task);

      expect($root.find("input[type=checkbox][value=passed]")).toBeChecked();
      expect($root.find("input[type=checkbox][value=failed]")).toBeChecked();
      expect($root.find("input[type=checkbox][value=any]")).not.toBeChecked();
    });
  });

  describe("Selection", () => {
    it("should be either 'any' or 'passed || failed'", () => {
      var task = new Tasks.Task.Exec({runIf: ['passed', 'failed']});
      mount(task);

      expect($root.find("input[type=checkbox][value=passed]")).toBeChecked();
      expect($root.find("input[type=checkbox][value=failed]")).toBeChecked();
      expect($root.find("input[type=checkbox][value=any]")).not.toBeChecked();

      $root.find("input[type=checkbox][value=any]").click();
      m.redraw();

      expect($root.find("input[type=checkbox][value=passed]")).not.toBeChecked();
      expect($root.find("input[type=checkbox][value=failed]")).not.toBeChecked();
      expect($root.find("input[type=checkbox][value=any]")).toBeChecked();
      expect(task.runIf().data()).toEqual(['any']);
      unmount();
    });
  });
});
