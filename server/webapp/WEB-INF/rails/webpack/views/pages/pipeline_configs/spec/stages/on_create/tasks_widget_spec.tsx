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
import {ExecTask, Task} from "models/new_pipeline_configs/task";
import {Tasks} from "models/new_pipeline_configs/tasks";
import {TasksTab} from "views/pages/pipeline_configs/stages/on_create/tasks_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Config - Job Settings Modal - Tasks Widget", () => {
  const helper = new TestHelper();
  let tasks: Stream<Tasks>;
  let lsTask: Task, sleepTask: Task;

  beforeEach(() => {
    lsTask    = new ExecTask("ls foo");
    sleepTask = new ExecTask("sleep 30");
    tasks     = Stream(new Tasks([lsTask, sleepTask]));
    helper.mount(() => <TasksTab tasks={tasks}/>);
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render the tasks tab", () => {
    expect(helper.byTestId("tasks-tab")).toBeInDOM();
  });

  it("should render first task by default", () => {
    expect(helper.textByTestId("selected-task")).toBe(lsTask.represent());
    expect(helper.text("pre")).toBe(lsTask.represent());
  });

  it("should display appropriate task in task editor on click of task from tasks list", () => {
    expect(helper.textByTestId("selected-task")).toBe(lsTask.represent());
    expect(helper.text("pre")).toBe(lsTask.represent());

    helper.click(helper.allByTestId("task-representation")[1]);

    expect(helper.textByTestId("selected-task")).toBe(sleepTask.represent());
    expect(helper.text("pre")).toBe(sleepTask.represent());
  });

  it("should select and render the next task when the currently selected task is deleted", () => {
    expect(helper.textByTestId("selected-task")).toBe(lsTask.represent());
    expect(helper.text("pre")).toBe(lsTask.represent());

    helper.clickByTestId("Delete-icon");

    expect(helper.textByTestId("selected-task")).toBe(sleepTask.represent());
    expect(helper.text("pre")).toBe(sleepTask.represent());
  });
});
