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

import {ExecTask, RunIfCondition, TaskType} from "models/new_pipeline_configs/task";

describe("Pipeline Config - Task Model", () => {
  describe("Exec", () => {
    let task: ExecTask;

    beforeEach(() => {
      task = new ExecTask("sleep 30");
    });

    it("should return the type of exec task", () => {
      expect(task.getType()).toEqual(TaskType.Exec);
    });

    it("should return the task command", () => {
      expect(task.command()).toEqual("sleep 30");
    });

    it("should allow setting task command", () => {
      expect(task.command()).toEqual("sleep 30");
      task.command("foo bar");
      expect(task.command()).toEqual("foo bar");
    });

    it("should represent the task", () => {
      expect(task.represent()).toEqual("sleep 30");
    });

    it("should set run if conditions to passed by default", () => {
      expect(task.runIfCondition()).toEqual(RunIfCondition.Passed);
    });

    it("should allow changing run if conditions", () => {
      expect(task.runIfCondition()).toEqual(RunIfCondition.Passed);
      task.runIfCondition(RunIfCondition.Any);
      expect(task.runIfCondition()).toEqual(RunIfCondition.Any);
    });

  });
});
