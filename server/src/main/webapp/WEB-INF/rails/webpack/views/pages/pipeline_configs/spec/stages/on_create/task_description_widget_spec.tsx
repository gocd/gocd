/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import m from "mithril";
import {ExecTask} from "models/new_pipeline_configs/task";
import {TaskDescriptionWidget} from "views/pages/pipeline_configs/stages/on_create/task_description_widget";
import task_terminal_styles from "views/pages/pipeline_configs/stages/on_create/task_terminal.scss";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Config - Job Settings Modal - Tasks Widget - Task Description", () => {
  const helper = new TestHelper();

  describe("Exec", () => {
    let task: ExecTask;

    beforeEach(() => {
      task = new ExecTask("ls foo");
      helper.mount(() => <TaskDescriptionWidget task={task}/>);
    });

    afterEach(() => {
      helper.unmount();
    });

    it("should render the task description", () => {
      expect(helper.byTestId("exec-task-description")).toBeInDOM();
    });

    describe("Task Editor", () => {
      it("should render the task editor", () => {
        expect(helper.q("code")).toBeInDOM();
      });

      it("should represent the task in the task editor window", () => {
        expect(helper.text("pre")).toBe(task.represent()!);
      });

      it("should render the caveats in the task editor", () => {
        expect(helper.byTestId("caveats-container")).toBeInDOM();
        expect(helper.textByTestId("caveats-container")).toContain("Caveats");
      });

      it("should display caveats description on click of expand icon", () => {
        const caveatsDesc = "This is not a real shell:";

        expect(helper.byTestId("caveats-container")).not.toHaveClass(task_terminal_styles.open);

        helper.clickByTestId("caveats-toggle-btn");

        expect(helper.byTestId("caveats-container")).toHaveClass(task_terminal_styles.open);
        expect(helper.textByTestId("caveats-container")).toContain(caveatsDesc);
      });

      it("should display editor instructions", () => {
        const caveatsInstructions = "# Press <enter> to save, <shift-enter> for newline";

        expect(helper.byTestId("caveats-instructions")).toBeInDOM();
        expect(helper.textByTestId("caveats-instructions")).toContain(caveatsInstructions);
      });
    });

    describe("run if conditions", () => {
      it("should render the run if conditions", () => {
        expect(helper.byTestId("run-if-conditions-container")).toBeInDOM();
      });

      it("should render all run if condition options", () => {
        const runIfConditionsContainer = helper.byTestId("run-if-conditions-container");

        expect(runIfConditionsContainer).toContainText("Run If Conditions");
        expect(runIfConditionsContainer).toContainText("Passed");
        expect(runIfConditionsContainer).toContainText("Failed");
        expect(runIfConditionsContainer).toContainText("Any");
      });

      it("should select passed by default", () => {
        expect(task.runIfCondition()).toBe("Passed");

        const passedRadioField = helper.byTestId("input-field-for-Passed");

        expect(helper.text("label", passedRadioField)).toContain(task.runIfCondition());
        expect(helper.q("input", passedRadioField)).toBeChecked();
      });

      it("should select another run if condition on click", () => {
        expect(task.runIfCondition()).toBe("Passed");

        const passedRadioField = helper.byTestId("input-field-for-Passed");

        expect(helper.text("label", passedRadioField)).toContain(task.runIfCondition());
        expect(helper.q("input", passedRadioField)).toBeChecked();

        click(helper.q("input", helper.byTestId("input-field-for-Failed")));

        const failedRadioField = helper.byTestId("input-field-for-Failed");
        expect(task.runIfCondition()).toBe("Failed");
        expect(helper.text("label", failedRadioField)).toContain(task.runIfCondition());
        expect(helper.q("input", failedRadioField)).toBeChecked();

        click(helper.q("input", helper.byTestId("input-field-for-Any")));

        const anyRadioField = helper.byTestId("input-field-for-Any");
        expect(task.runIfCondition()).toBe("Any");
        expect(helper.text("label", anyRadioField)).toContain(task.runIfCondition());
        expect(helper.q("input", anyRadioField)).toBeChecked();
      });
    });

    function click(el: Element) {
      (el as HTMLElement).click();
      helper.redraw();
    }
  });

});
