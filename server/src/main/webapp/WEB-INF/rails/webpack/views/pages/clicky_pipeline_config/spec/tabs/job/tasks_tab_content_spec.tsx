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

import m from "mithril";
import Stream from "mithril/stream";
import {Job} from "models/pipeline_configs/job";
import {JobTestData} from "models/pipeline_configs/spec/test_data";
import {ExecTask, ExecTaskAttributes} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {TasksWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab_content";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Tasks Tab Content", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render table header", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(helper.qa("th", helper.byTestId("table-header-row"))[1]).toContainText("Task Type");
    expect(helper.qa("th", helper.byTestId("table-header-row"))[2]).toContainText("Run If Condition");
    expect(helper.qa("th", helper.byTestId("table-header-row"))[3]).toContainText("Properties");
    expect(helper.qa("th", helper.byTestId("table-header-row"))[4]).toContainText("On Cancel");
    expect(helper.qa("th", helper.byTestId("table-header-row"))[5]).toContainText("Remove");
  });

  it("should remove task", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.tasks().push(new ExecTask("ls", ["-a", "-l", "-h"]));
    mount(job);

    helper.qa("td", helper.byTestId("table-row"));

    expect(helper.allByTestId("table-row")).toHaveLength(1);

    helper.click(`[data-test-id="Delete-icon"]`);

    expect(helper.allByTestId("table-row")).toHaveLength(0);
  });

  describe("Exec Task", () => {
    let job: Job;
    beforeEach(() => {
      job = Job.fromJSON(JobTestData.with("test"));
      job.tasks().push(new ExecTask("ls", ["-a", "-l", "-h"]));
      mount(job);
    });

    it("should render exec task type", () => {
      const rows = helper.qa("td", helper.byTestId("table-row"));
      expect(rows[1]).toContainText("Custom Command");
    });

    it("should render failed run if condition", () => {
      job.tasks()[0].attributes().runIf(["failed"]);
      helper.redraw();

      const rows = helper.qa("td", helper.byTestId("table-row"));
      expect(rows[2]).toContainText("failed");
    });

    it("should render properties", () => {
      expect(helper.byTestId("key-value-key-command")).toContainText("Command");
      expect(helper.byTestId("key-value-value-command")).toContainText("ls");
      expect(helper.byTestId("key-value-key-arguments")).toContainText("Arguments");
      expect(helper.byTestId("key-value-value-arguments")).toContainText("-a -l -h");
      expect(helper.byTestId("key-value-key-working-directory")).toContainText("Working Directory");
      expect(helper.byTestId("key-value-value-working-directory")).toContainText("(Not specified)");
    });

    it("should render working directory property when specified", () => {
      (job.tasks()[0].attributes() as ExecTaskAttributes).workingDirectory("/tmp");
      helper.redraw();

      expect(helper.byTestId("key-value-key-working-directory")).toContainText("Working Directory");
      expect(helper.byTestId("key-value-value-working-directory")).toContainText("/tmp");
    });

    it("should render no on cancel specified", () => {
      const rows = helper.qa("td", helper.byTestId("table-row"));
      expect(rows[4]).toContainText("No");
    });

    it("should render on cancel task type", () => {
      (job.tasks()[0].attributes() as ExecTaskAttributes).onCancel(new ExecTask("ls", []));
      helper.redraw();

      const rows = helper.qa("td", helper.byTestId("table-row"));
      expect(rows[4]).toContainText("exec");
    });
  });

  function mount(job: Job) {
    helper.mount(() => {
      return <TasksWidget tasks={job.tasks}
                          isEditable={true}
                          autoSuggestions={Stream({})}
                          pluginInfos={Stream(new PluginInfos())}/>;
    });
  }
});
