/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import Stream from "mithril/stream";
import {AntTask, Task} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {OnCancelTaskWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/common/on_cancel_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("On Cancel Task Widget", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should not render any on cancel task when none specified", () => {
    const task: Stream<Task | undefined> = Stream();
    mount(task);

    const helpText = "Task which needs to be run, if the parent task is cancelled. Note: The default action of killing the parent task will not be performed.";
    expect(helper.byTestId("on-cancel-view")).toBeInDOM();
    expect(helper.byTestId("form-field-input-on-cancel-task")).not.toBeChecked();
    expect(helper.byTestId("on-cancel-view")).toContainText(helpText);

    expect(helper.byTestId("on-cancel-body")).not.toBeInDOM();
  });

  it("should render on cancel task when specified", () => {
    const task: Stream<Task> = Stream(new AntTask("build.xml", "default", "pwd", []));
    mount(task as Stream<Task | undefined>);

    expect(helper.byTestId("on-cancel-view")).toBeInDOM();
    expect(helper.byTestId("form-field-input-on-cancel-task")).toBeChecked();
    expect(helper.byTestId("on-cancel-body")).toBeInDOM();

    expect(helper.byTestId("form-field-input-build-file")).toHaveValue("build.xml");
    expect(helper.byTestId("form-field-input-target")).toHaveValue("default");
    expect(helper.byTestId("form-field-input-working-directory")).toHaveValue("pwd");
  });

  describe("Read Only", () => {
    beforeEach(() => {
      const task: Stream<Task> = Stream(new AntTask("build.xml", "default", "pwd", []));
      mount(task as Stream<Task | undefined>, true);
    });

    it("should render disabled any on cancel task", () => {
      expect(helper.byTestId("form-field-input-on-cancel-task")).toBeChecked();
      expect(helper.byTestId("form-field-input-on-cancel-task")).toBeDisabled();
    });

    it("should render disabled task type checkbox", () => {
      expect(helper.byTestId("form-field-input-")).toBeDisabled();
    });

    it("should render disabled on cancel task", () => {
      expect(helper.byTestId("form-field-input-build-file")).toBeDisabled();
      expect(helper.byTestId("form-field-input-target")).toBeDisabled();
      expect(helper.byTestId("form-field-input-working-directory")).toBeDisabled();
    });
  });

  const mount = (onCancel: Stream<Task | undefined>, readonly: boolean = false) => {
    helper.mount(() => {
      return <OnCancelTaskWidget onCancel={onCancel} pluginInfos={new PluginInfos()} readonly={readonly}/>;
    });
  };
});
