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

import {ExecTask} from "models/new_pipeline_configs/task";
import {Tasks} from "models/new_pipeline_configs/tasks";

describe("Pipeline Config - Tasks Model", () => {
  it("should count the tasks", () => {
    expect(new Tasks().count()).toBe(0);
    expect(new Tasks([new ExecTask("ls")]).count()).toBe(1);
  });

  it("should allow adding a task", () => {
    const tasks = new Tasks([new ExecTask("ls")]);
    expect(tasks.count()).toBe(1);
    tasks.add(new ExecTask("sleep 30"));
    expect(tasks.count()).toBe(2);
  });

  it("should return the list of tasks", () => {
    const lsTask    = new ExecTask("ls");
    const sleepTask = new ExecTask("sleep 30");
    const tasks     = new Tasks([lsTask, sleepTask]);

    expect(tasks.list()).toEqual([lsTask, sleepTask]);
  });

  it("should allow removing a task", () => {
    const lsTask    = new ExecTask("ls");
    const sleepTask = new ExecTask("sleep 30");
    const tasks     = new Tasks([lsTask, sleepTask]);

    expect(tasks.list()).toEqual([lsTask, sleepTask]);
    tasks.remove(sleepTask);
    expect(tasks.list()).toEqual([lsTask]);
  });
});
