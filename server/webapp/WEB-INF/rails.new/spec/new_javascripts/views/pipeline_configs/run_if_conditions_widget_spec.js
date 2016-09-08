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

define(['jquery', "mithril", "lodash", "views/pipeline_configs/run_if_conditions_widget", "models/pipeline_configs/tasks"], function ($, m, _, RunIfConditionsWidget, Tasks) {
  describe("RunIfConditions Widget", function() {
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    function mount(task) {
      m.mount(root,
        m.component(RunIfConditionsWidget, {task: task})
      );
      m.redraw(true);
    }

    describe("View", function() {

      it("should render checkbox for runIf conditions", function () {
        var task = new Tasks.Task.Exec({runIf: ['any']});
        mount(task);

        expect($root.find("input[type=checkbox][value=passed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=failed]").size()).toBe(1);
        expect($root.find("input[type=checkbox][value=any]").size()).toBe(1);
      });

      it("should have run_if conditions checked for tasks with runIf", function() {
        var task = new Tasks.Task.Exec({runIf: ['passed', 'failed']});
        mount(task);

        expect($root.find("input[type=checkbox][value=passed]").is(':checked')).toBe(true);
        expect($root.find("input[type=checkbox][value=failed]").is(':checked')).toBe(true);
        expect($root.find("input[type=checkbox][value=any]").is(':checked')).toBe(false);
      });
    });

    describe("Selection", function() {
      it("should be either 'any' or 'passed || failed'", function() {
        var task = new Tasks.Task.Exec({runIf: ['passed', 'failed']});
        mount(task);

        expect($root.find("input[type=checkbox][value=passed]").is(':checked')).toBe(true);
        expect($root.find("input[type=checkbox][value=failed]").is(':checked')).toBe(true);
        expect($root.find("input[type=checkbox][value=any]").is(':checked')).toBe(false);

        $root.find("input[type=checkbox][value=any]").click();
        m.redraw(true);

        expect($root.find("input[type=checkbox][value=passed]").is(':checked')).toBe(false);
        expect($root.find("input[type=checkbox][value=failed]").is(':checked')).toBe(false);
        expect($root.find("input[type=checkbox][value=any]").is(':checked')).toBe(true);
        expect(task.runIf().data()).toEqual(['any']);
      });
    });
  });
});
