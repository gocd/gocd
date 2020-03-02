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

import {Task} from "models/pipeline_configs/task";
import {TaskExtension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {TaskPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import {PluggableTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/plugin";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pluggable Task Modal", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render no task plugin installed message", () => {
    mount();
    const expectedMsg = "Can not define plugin task as no task plugins are installed!";
    expect(helper.byTestId("flash-message-info")).toContainText(expectedMsg);
  });

  it("should render task plugin view", () => {
    mount(undefined, true, new PluginInfos(PluginInfo.fromJSON(TaskPluginInfo.scriptExecutor())));
    expect(helper.byTestId("task-plugin-modal")).toBeInDOM();
  });

  it("should render task plugin selection", () => {
    mount(undefined, true, new PluginInfos(PluginInfo.fromJSON(TaskPluginInfo.scriptExecutor())));

    expect(helper.byTestId("form-field-label-select-task-plugin")).toContainText("Select Task Plugin");
    expect(helper.byTestId("form-field-input-select-task-plugin")).toHaveValue("script-executor");
  });

  it("should render plugin view", () => {
    const pluginInfo = PluginInfo.fromJSON(TaskPluginInfo.scriptExecutor());
    mount(undefined, true, new PluginInfos(pluginInfo));

    expect(helper.byTestId("task-plugin-modal"))
      .toContainText((pluginInfo.extensions[0] as TaskExtension).taskSettings!.viewTemplate()!);
  });

  it("should render run if condition", () => {
    mount(undefined, true, new PluginInfos(PluginInfo.fromJSON(TaskPluginInfo.scriptExecutor())));
    expect(helper.byTestId("run-if-condition")).toBeInDOM();
  });

  it("should render run on cancel", () => {
    mount(undefined, true, new PluginInfos(PluginInfo.fromJSON(TaskPluginInfo.scriptExecutor())));
    expect(helper.byTestId("on-cancel-view")).toBeInDOM();
  });

  it("should not render run if condition for on cancel task", () => {
    mount(undefined, false, new PluginInfos(PluginInfo.fromJSON(TaskPluginInfo.scriptExecutor())));

    expect(helper.byTestId("ant-on-cancel-view")).not.toBeInDOM();
    expect(helper.byTestId("run-if-condition")).toBeFalsy();
  });

  it("should not render on cancel for on cancel task", () => {
    mount(undefined, false, new PluginInfos(PluginInfo.fromJSON(TaskPluginInfo.scriptExecutor())));

    expect(helper.byTestId("ant-on-cancel-view")).not.toBeInDOM();
    expect(helper.byTestId("on-cancel-view")).toBeFalsy();
  });

  function mount(task?: Task | undefined,
                 shouldShowOnCancel: boolean = true,
                 pluginInfos: PluginInfos    = new PluginInfos()) {

    helper.mount(() => new PluggableTaskModal(task, shouldShowOnCancel, jasmine.createSpy(), pluginInfos).body());
  }
});
