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

import {RakeTask, RakeTaskAttributes, Task} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {RakeTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/rake";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Rake Task Modal", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render rake task modal", () => {
    mount();
    expect(helper.byTestId("rake-task-modal")).toBeInDOM();
  });

  it("should render build file input", () => {
    mount();

    const buildFileHelpText = "Path to Rake file. If not specified, the path defaults to 'rakefile'.";

    expect(helper.byTestId("form-field-label-build-file")).toContainText("Build File");
    expect(helper.byTestId("form-field-input-build-file")).toBeInDOM();
    expect(helper.byTestId("form-field-input-build-file")).toBeInDOM();
    expect(helper.qa("span")[0]).toContainText(buildFileHelpText);
  });

  it("should bind build file input to model", () => {
    const rakeTask = new RakeTask("build.xml", "target", "/tmp", []);
    mount(rakeTask);

    const attributes = rakeTask.attributes() as RakeTaskAttributes;

    expect(attributes.buildFile()).toBe("build.xml");
    expect(helper.byTestId("form-field-input-build-file")).toHaveValue("build.xml");

    helper.oninput(`[data-test-id="form-field-input-build-file"]`, "new-build.xml");

    expect(attributes.buildFile()).toBe("new-build.xml");
    expect(helper.byTestId("form-field-input-build-file")).toHaveValue("new-build.xml");
  });

  it("should render target input", () => {
    mount();

    const buildFileHelpText = "Rake target(s) to run.";

    expect(helper.byTestId("form-field-label-target")).toContainText("Target");
    expect(helper.byTestId("form-field-input-target")).toBeInDOM();
    expect(helper.byTestId("form-field-input-target")).toBeInDOM();
    expect(helper.qa("span")[1]).toContainText(buildFileHelpText);
  });

  it("should bind target input to model", () => {
    const rakeTask = new RakeTask("build.xml", "default", "/tmp", []);
    mount(rakeTask);

    const attributes = rakeTask.attributes() as RakeTaskAttributes;

    expect(attributes.target()).toBe("default");
    expect(helper.byTestId("form-field-input-target")).toHaveValue("default");

    helper.oninput(`[data-test-id="form-field-input-target"]`, "new-default");

    expect(attributes.target()).toBe("new-default");
    expect(helper.byTestId("form-field-input-target")).toHaveValue("new-default");
  });

  it("should render working directory input", () => {
    mount();

    const buildFileHelpText = "The directory from where rake is invoked.";

    expect(helper.byTestId("form-field-label-working-directory")).toContainText("Working Directory");
    expect(helper.byTestId("form-field-input-working-directory")).toBeInDOM();
    expect(helper.byTestId("form-field-input-working-directory")).toBeInDOM();
    expect(helper.qa("span")[2]).toContainText(buildFileHelpText);
  });

  it("should bind working directory input to model", () => {
    const rakeTask = new RakeTask("build.xml", "target", "tmp", []);
    mount(rakeTask);

    const attributes = rakeTask.attributes() as RakeTaskAttributes;

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

    expect(helper.byTestId("rake-on-cancel-view")).not.toBeInDOM();
    expect(helper.byTestId("run-if-condition")).toBeFalsy();
  });

  it("should not render on cancel for on cancel task", () => {
    mount(undefined, false);

    expect(helper.byTestId("rake-on-cancel-view")).not.toBeInDOM();
    expect(helper.byTestId("on-cancel-view")).toBeFalsy();
  });

  function mount(task?: Task | undefined, shouldShowOnCancel: boolean = true) {
    helper.mount(() => {
      return new RakeTaskModal(task, shouldShowOnCancel, jasmine.createSpy(), new PluginInfos()).body();
    });
  }
});
