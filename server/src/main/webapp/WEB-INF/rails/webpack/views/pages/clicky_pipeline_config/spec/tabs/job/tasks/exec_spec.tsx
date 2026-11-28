/*
 * Copyright Thoughtworks, Inc.
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

    expect(helpTextFor("command")).toContainText(commandHelpText);
  });

  it("should bind command input to model", () => {
    const execTask = new ExecTask("ls", [], undefined, "tmp", []);
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
    expect(helpTextFor("arguments")).toContainText(argumentsHelpText);
    expect(helpTextFor("arguments")).toContainText("-c");
    expect(helpTextFor("arguments")).toContainText("echo hello");
  });

  it("shows the argument boundaries without splitting spaces within an argument", () => {
    mount(new ExecTask("bash", ["-c", "echo hello"], undefined, undefined, []));

    expect(helper.textAll("[data-test-id='exec-arguments-preview'] li")).toEqual(['"-c"', '"echo hello"']);
    expect(helper.byTestId("exec-combined-flag-argument-warning")).toBeNull();
  });

  it("suggests removing leading spaces and detects flags after those spaces", () => {
    const task = new ExecTask("bash", ["  -c echo hello", " \tother argument"], undefined, undefined, []);
    mount(task);

    expect(helper.byTestId("exec-leading-spaces-warning")).toContainText("removed when you save");
    expect(helper.textAll("[data-test-id='exec-arguments-preview'] li")).toEqual(['"-c echo hello"', '"other argument"']);
    expect(helper.byTestId("exec-combined-flag-argument-warning")).toContainText("-c");
    expect((task.attributes() as ExecTaskAttributes).arguments()).toEqual(["  -c echo hello", " \tother argument"]);

    helper.click(helper.byTestId("remove-leading-spaces"));

    expect((task.attributes() as ExecTaskAttributes).arguments()).toEqual(["-c echo hello", "other argument"]);
    expect(helper.byTestId("exec-leading-spaces-warning")).toBeNull();
    expect(helper.byTestId("exec-combined-flag-argument-warning")).toContainText("-c");
  });

  it("removes leading spaces from every argument line before saving", () => {
    const task  = new ExecTask("bash", ["  -c", " \techo hello", "plain argument"], undefined, undefined, []);
    const onAdd = jasmine.createSpy("onAdd").and.returnValue(Promise.resolve());
    mount(task, true, false, onAdd);

    expect(helper.byTestId("form-field-input-arguments")).toHaveValue("  -c\n \techo hello\nplain argument");

    modal.addTaskAndSave();

    expect((task.attributes() as ExecTaskAttributes).arguments()).toEqual(["-c", "echo hello", "plain argument"]);
    expect(onAdd).toHaveBeenCalledWith(task);
  });

  it("warns about a combined bash -c argument and can split it", () => {
    const task = new ExecTask("bash", ["-c echo hello"], undefined, undefined, []);
    mount(task);

    expect(helper.textAll("[data-test-id='exec-arguments-preview'] li")).toEqual(['"-c echo hello"']);
    expect(helper.byTestId("exec-combined-flag-argument-warning")).toContainText("-c");

    helper.click(helper.byTestId("split-flag-argument"));

    expect((task.attributes() as ExecTaskAttributes).arguments()).toEqual(["-c", "echo hello"]);
    expect(helper.byTestId("form-field-input-arguments")).toHaveValue("-c\necho hello");
    expect(helper.byTestId("exec-combined-flag-argument-warning")).toBeNull();
  });

  it("offers to split a long flag after another argument for any command", () => {
    const task = new ExecTask("my-tool", ["input.txt", "--output build dir"], undefined, undefined, []);
    mount(task);

    expect(helper.byTestId("exec-combined-flag-argument-warning")).toContainText("Argument 2");
    expect(helper.byTestId("exec-combined-flag-argument-warning")).toContainText("--output");

    helper.click(helper.byTestId("split-flag-argument"));

    expect((task.attributes() as ExecTaskAttributes).arguments()).toEqual(["input.txt", "--output", "build dir"]);
  });

  it("offers to split before a flag embedded after other text on the same line", () => {
    const task = new ExecTask("my-tool", ["input.txt --output build.dir"], undefined, undefined, []);
    mount(task);

    expect(helper.textAll("[data-test-id='exec-arguments-preview'] li")).toEqual(['"input.txt --output build.dir"']);
    expect(helper.byTestId("exec-combined-flag-argument-warning")).toContainText("--output");
    expect(helper.byTestId("split-flag-argument")).toContainText("Split at --output");

    helper.click(helper.byTestId("split-flag-argument"));

    expect((task.attributes() as ExecTaskAttributes).arguments()).toEqual(["input.txt", "--output", "build.dir"]);
    expect(helper.byTestId("form-field-input-arguments")).toHaveValue("input.txt\n--output\nbuild.dir");
    expect(helper.byTestId("exec-combined-flag-argument-warning")).toBeNull();
  });

  it("keeps a value joined to an embedded flag with an equals sign", () => {
    const task = new ExecTask("my-tool", ["input.txt --output=build.dir"], undefined, undefined, []);
    mount(task);

    helper.click(helper.byTestId("split-flag-argument"));

    expect((task.attributes() as ExecTaskAttributes).arguments()).toEqual(["input.txt", "--output=build.dir"]);
  });

  it("does not warn about spaces without a leading flag or a flag on its own line", () => {
    mount(new ExecTask("echo", ["--", "-n", "hello world"], undefined, undefined, []));

    expect(helper.byTestId("exec-combined-flag-argument-warning")).toBeNull();
  });

  it("should bind arguments input to model via array", () => {
    const execTask = new ExecTask("ls", ["-a", "-h"], undefined,"tmp", []);
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

  it("should render single line argument input", () => {
    const execTask = new ExecTask("ls", [], "-alh", "tmp", []);
    mount(execTask);

    const argumentsHelpText = "command arguments";

    expect(helper.byTestId("form-field-label-arguments")).toContainText("Arguments");
    expect(helper.byTestId("form-field-input-arguments")).toBeInDOM();
    expect(helper.byTestId("form-field-input-arguments")).toBeInDOM();
    expect(helpTextFor("arguments")).toContainText(argumentsHelpText);
  });

  it("should bind arguments input to model via string", () => {
    const execTask = new ExecTask("ls", [], "-alh", "tmp", []);
    mount(execTask);

    let attributes = modal.getTask().attributes() as ExecTaskAttributes;

    expect(attributes.args()).toEqual("-alh");

    const input = helper.byTestId("form-field-input-arguments") as HTMLInputElement;
    input.value = "-a";
    simulateEvent.simulate(input, "input");

    attributes = modal.getTask().attributes() as ExecTaskAttributes;

    expect(attributes.args()).toEqual("-a");
    expect(helper.byTestId("form-field-input-arguments")).toHaveValue("-a");
  });

  it("should render working directory input", () => {
    mount();

    const buildFileHelpText = "The directory in which the script or command is to be executed. This is always relative to the directory where the agent checks out materials.";

    expect(helper.byTestId("form-field-label-working-directory")).toContainText("Working Directory");
    expect(helper.byTestId("form-field-input-working-directory")).toBeInDOM();
    expect(helper.byTestId("form-field-input-working-directory")).toBeInDOM();
    expect(helpTextFor("working-directory")).toContainText(buildFileHelpText);
  });

  it("should bind working directory input to model", () => {
    const execTask = new ExecTask("ls", [], undefined, "tmp", []);
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

  describe("Read Only", () => {
    beforeEach(() => {
      mount(undefined, false, true);
    });

    it("should render read only command", () => {
      expect(helper.byTestId("form-field-input-command")).toBeDisabled();
    });

    it("should render read only arguments", () => {
      expect(helper.byTestId("form-field-input-arguments")).toBeDisabled();
    });

    it("should render read only working directory", () => {
      expect(helper.byTestId("form-field-input-working-directory")).toBeDisabled();
    });
  });

  function mount(task?: Task | undefined, shouldShowOnCancel: boolean = true, readonly: boolean = false,
                 onAdd: (t: Task) => Promise<any> = () => Promise.resolve()) {
    helper.mount(() => {
      modal = new ExecTaskModal(task, shouldShowOnCancel, onAdd, new PluginInfos(), readonly);
      return modal.body();
    });
  }

  function helpTextFor(field: string) {
    const helpTextId = helper.byTestId(`form-field-input-${field}`).getAttribute("aria-describedby");
    return helper.q(`#${helpTextId}`);
  }
});
