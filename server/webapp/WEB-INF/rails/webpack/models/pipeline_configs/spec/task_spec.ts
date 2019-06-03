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

import {ExecTask, Task} from "models/pipeline_configs/task";

describe("Task", () => {
  it("validates attributes", () => {
    const t: Task = new ExecTask("", []);
    expect(t.isValid()).toBe(false);
    expect(t.attributes().errors().count()).toBe(1);
    expect(t.attributes().errors().keys()).toEqual(["command"]);
    expect(t.attributes().errors().errorsForDisplay("command")).toBe("Command must be present.");

    expect(new ExecTask("ls", ["-lA"]).isValid());
  });

  it("adopts errors in server response", () => {
    const task = new ExecTask("whoami", []);

    const unmatched = task.consumeErrorsResponse({
      errors: { command: ["who are you?"], not_exist: ["well, ain't that a doozy"] }
    });

    expect(unmatched.hasErrors()).toBe(true);
    expect(unmatched.errorsForDisplay("execTask.notExist")).toBe("well, ain't that a doozy.");

    expect(task.attributes().errors().errorsForDisplay("command")).toBe("who are you?.");
  });

  it("serializes", () => {
    expect(new ExecTask("ls", ["-la"]).toJSON()).toEqual({
      type: "exec",
      attributes: {
        command: "ls",
        arguments: ["-la"],
        run_if: []
      }
    });
  });
});
