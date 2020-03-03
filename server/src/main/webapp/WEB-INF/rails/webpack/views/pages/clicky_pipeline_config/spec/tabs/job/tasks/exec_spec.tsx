/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {ExecTask, ExecTaskAttributes, Task} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as simulateEvent from "simulate-event";
import {ExecTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/exec";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Exec Task Modal", () => {
  const helper = new TestHelper();

  let modal: ExecTaskModal;

  afterEach(helper.unmount.bind(helper));

  it("should render exec task modal", () => {
    mount();
    expect(helper.byTestId("exec-task-modal")).toBeInDOM();
  });

  it("should render command input", () => {
    mount();

    const commandHelpText = "The command or script to be executed, relative to the working directory";

    expect(helper.byTestId("form-field-label-command")).toContainText("Command");
    expect(helper.byTestId("form-field-input-command")).toBeInDOM();
    expect(helper.byTestId("form-field-input-command")).toBeInDOM();

    expect(helper.qa("span")[1]).toContainText(commandHelpText);
  });

  it("should bind command input to model", () => {
    const execTask = new ExecTask("ls", [], "tmp", []);
    mount(execTask);

    const attributes = execTask.attributes() as ExecTaskAttributes;

    expect(attributes.command()).toBe("ls");
    expect(helper.byTestId("form-field-input-command")).toHaveValue("ls");

    helper.oninput(`[data-test-id="form-field-input-command"]`, "new-ls");

    expect(attributes.command()).toBe("new-ls");
    expect(helper.byTestId("form-field-input-command")).toHaveValue("new-ls");
  });

  it("should render arguments input", () => {
    mount();

    const argumentsHelpText = "Enter each argument on a new line";

    expect(helper.byTestId("form-field-label-arguments")).toContainText("Arguments");
    expect(helper.byTestId("form-field-input-arguments")).toBeInDOM();
    expect(helper.byTestId("form-field-input-arguments")).toBeInDOM();
    expect(helper.qa("span")[2]).toContainText(argumentsHelpText);
  });

  it("should bind arguments input to model", () => {
    const execTask = new ExecTask("ls", ["-a", "-h"], "tmp", []);
    mount(execTask);

    let attributes = modal.getTask().attributes() as ExecTaskAttributes;

    expect(attributes.arguments()).toEqual(["-a", "-h"]);

    const input = helper.byTestId("form-field-input-arguments") as HTMLInputElement;
    input.value = "-alh";
    simulateEvent.simulate(input, "input");

    attributes = modal.getTask().attributes() as ExecTaskAttributes;

    expect(attributes.arguments()).toEqual(["-alh"]);
    expect(helper.byTestId("form-field-input-arguments")).toHaveValue("-alh");
  });

  it("should render working directory input", () => {
    mount();

    const buildFileHelpText = "The directory in which the script or command is to be executed. This is always relative to the directory where the agent checks out materials.";

    expect(helper.byTestId("form-field-label-working-directory")).toContainText("Working Directory");
    expect(helper.byTestId("form-field-input-working-directory")).toBeInDOM();
    expect(helper.byTestId("form-field-input-working-directory")).toBeInDOM();
    expect(helper.qa("span")[3]).toContainText(buildFileHelpText);
  });

  it("should bind working directory input to model", () => {
    const execTask = new ExecTask("ls", [], "tmp", []);
    mount(execTask);

    const attributes = execTask.attributes() as ExecTaskAttributes;

    expect(attributes.workingDirectory()).toBe("tmp");
    expect(helper.byTestId("form-field-input-working-directory")).toHaveValue("tmp");

    helper.oninput(`[data-test-id="form-field-input-working-directory"]`, "new-tmp");

    expect(attributes.workingDirectory()).toBe("new-tmp");
    expect(helper.byTestId("form-field-input-working-directory")).toHaveValue("new-tmp");
  });

  it("should render run if condition", () => {
    mount();
    expect(helper.byTestId("run-if-condition")).toBeInDOM();
  });

  it("should render run on cancel", () => {
    mount();
    expect(helper.byTestId("on-cancel-view")).toBeInDOM();
  });

  it("should not render run if condition for on cancel task", () => {
    mount(undefined, false);

    expect(helper.byTestId("exec-on-cancel-view")).not.toBeInDOM();
    expect(helper.byTestId("run-if-condition")).toBeFalsy();
  });

  it("should not render on cancel for on cancel task", () => {
    mount(undefined, false);

    expect(helper.byTestId("exec-on-cancel-view")).not.toBeInDOM();
    expect(helper.byTestId("on-cancel-view")).toBeFalsy();
  });

  function mount(task?: Task | undefined, shouldShowOnCancel: boolean = true) {
    helper.mount(() => {
      modal = new ExecTaskModal(task, shouldShowOnCancel, jasmine.createSpy(), new PluginInfos());
      return modal.body();
    });
  }
});
