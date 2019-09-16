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
import Stream from "mithril/stream";
import {ExecTask} from "models/new_pipeline_configs/task";
import {Tasks} from "models/new_pipeline_configs/tasks";
import {TasksTab} from "views/pages/pipeline_configs/stages/on_create/tasks_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Config - Job Settings Modal - Tasks Widget - Tasks List", () => {
  const helper = new TestHelper();
  let tasks: Stream<Tasks>;

  beforeEach(() => {
    tasks = Stream(new Tasks([
                               new ExecTask("ls foo"),
                               new ExecTask("sleep 30")
                             ]));
    helper.mount(() => <TasksTab tasks={tasks}/>);
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render the tasks list", () => {
    expect(helper.byTestId("tasks-list")).toBeInDOM();
  });

  it("should list all tasks", () => {
    const tasks = helper.allByTestId("task-representation");

    expect(tasks).toHaveLength(2);
    expect(tasks[0]).toHaveText("ls foo");
    expect(tasks[1]).toHaveText("sleep 30");
  });

  it("should render delete icon for all the tasks", () => {
    const deleteIcons = helper.allByTestId("Delete-icon");

    expect(deleteIcons).toHaveLength(2);
  });

  it("should delete a task", () => {
    expect(helper.textByTestId("task-representation")).toContain("ls foo");

    helper.clickByTestId("Delete-icon");

    const firstTask = helper.byTestId("task-representation");

    expect(firstTask.textContent).not.toContain("ls foo");
    expect(firstTask.textContent).toContain("sleep 30");
  });
});
